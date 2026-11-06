package isp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Background reports run on a bounded thread pool and publish progress to waiting readers. */
public class ReportService implements AutoCloseable {
    private final IspService service;
    private final ReportFileStore reportFiles = new ReportFileStore();
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Map<String, ReportJob> jobs = new ConcurrentHashMap<>();

    public ReportService(IspService service) { this.service = service; }

    public Map<String, Object> start() {
        String id = UUID.randomUUID().toString();
        ReportJob job = new ReportJob(id);
        jobs.put(id, job);
        Future<?> future = pool.submit(() -> {
            try {
                job.update("RUNNING", 15, "Collecting customer and subscription data.", null);
                Thread.sleep(350);
                job.update("RUNNING", 55, "Summarizing billing and support tickets.", null);
                Thread.sleep(350);
                String report = service.buildReport();
                try { reportFiles.writeReport(id, report); }
                catch (IOException exception) { throw new UncheckedIOException("Could not write report file.", exception); }
                job.update("COMPLETED", 100, "Report is ready to download.", report);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                job.update("CANCELLED", 0, "Report generation was cancelled.", null);
            } catch (RuntimeException failure) {
                job.update("FAILED", 0, failure.getMessage() == null ? "Report generation failed." : failure.getMessage(), null);
            }
        });
        job.setFuture(future);
        return job.snapshot();
    }

    public Map<String, Object> read(String id, int knownVersion, long waitMillis) throws InterruptedException {
        ReportJob job = jobs.get(id);
        if (job == null) throw new IllegalArgumentException("Report job was not found.");
        return job.awaitSnapshot(knownVersion, waitMillis);
    }

    public Map<String, Object> cancel(String id) {
        ReportJob job = jobs.get(id);
        if (job == null) throw new IllegalArgumentException("Report job was not found.");
        job.cancel();
        return job.snapshot();
    }

    public String readReportFile(String id) throws IOException {
        return reportFiles.readReport(id);
    }

    @Override public void close() { pool.shutdownNow(); }

    private static final class ReportJob {
        private final String id;
        private String status = "QUEUED";
        private int progress;
        private String message = "Waiting for a report worker.";
        private String result;
        private int version;
        private Future<?> future;

        ReportJob(String id) { this.id = id; }
        synchronized void setFuture(Future<?> future) { this.future = future; }

        synchronized void update(String status, int progress, String message, String result) {
            if (isTerminal()) return;
            this.status = status; this.progress = progress; this.message = message; this.result = result;
            version++; notifyAll();
        }

        synchronized Map<String, Object> awaitSnapshot(int knownVersion, long waitMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + waitMillis;
            while (version <= knownVersion && !isTerminal() && waitMillis > 0) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                wait(remaining);
            }
            return snapshot();
        }

        synchronized Map<String, Object> snapshot() {
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("id", id); response.put("status", status); response.put("progress", progress);
            response.put("message", message); response.put("version", version);
            if (result != null) response.put("result", result);
            return response;
        }

        synchronized void cancel() {
            if (isTerminal()) return;
            if (future != null) future.cancel(true);
            status = "CANCELLED"; progress = 0; message = "Report generation was cancelled."; version++;
            notifyAll();
        }

        private boolean isTerminal() { return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status); }
    }
}
