package isp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Writes and reads generated admin reports under the project's backend/exports folder. */
public final class ReportFileStore {
    private final Path exportDirectory = Path.of("backend", "exports").toAbsolutePath().normalize();

    public Path writeReport(String reportId, String content) throws IOException {
        Path report = reportPath(reportId);
        Files.createDirectories(exportDirectory);
        Files.writeString(report, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return report;
    }

    public String readReport(String reportId) throws IOException {
        return Files.readString(reportPath(reportId), StandardCharsets.UTF_8);
    }

    private Path reportPath(String reportId) {
        if (reportId == null || !reportId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("Invalid report ID.");
        }
        Path report = exportDirectory.resolve("admin-report-" + reportId + ".txt").normalize();
        if (!report.startsWith(exportDirectory)) throw new IllegalArgumentException("Invalid report path.");
        return report;
    }
}
