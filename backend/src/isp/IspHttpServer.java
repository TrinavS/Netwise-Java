package isp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Same-origin HTTP API and static frontend host; no third-party libraries required. */
public class IspHttpServer implements AutoCloseable {
    private static final Pattern USAGE_PATH = Pattern.compile("^/api/customers/(\\d+)/usage$");
    private static final Pattern COMPLAINT_PATH = Pattern.compile("^/api/complaints/([^/]+)$");
    private static final Pattern BILL_PATH = Pattern.compile("^/api/bills/([^/]+)/pay$");
    private static final Pattern REPORT_PATH = Pattern.compile("^/api/reports/([^/]+)$");
    private static final Pattern REPORT_CANCEL_PATH = Pattern.compile("^/api/reports/([^/]+)/cancel$");

    private final IspService service;
    private final ReportService reports;
    private final HttpServer server;
    private final ExecutorService requestPool = Executors.newCachedThreadPool();
    private final Path frontendRoot;

    public IspHttpServer(IspService service, String host, int port, Path frontendRoot) throws IOException {
        this.service = service;
        this.reports = new ReportService(service);
        this.frontendRoot = frontendRoot.toAbsolutePath().normalize();
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.setExecutor(requestPool);
        server.createContext("/api", this::handleApi);
        server.createContext("/", this::handleStatic);
    }

    public void start() { server.start(); }
    public int getPort() { return server.getAddress().getPort(); }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("OPTIONS".equals(method)) { send(exchange, 204, ""); return; }
            if ("GET".equals(method) && "/api/state".equals(path)) { json(exchange, 200, service.snapshot()); return; }
            if ("GET".equals(method) && "/api/health".equals(path)) { json(exchange, 200, Map.of("status", "ok")); return; }
            if ("POST".equals(method) && "/api/customers".equals(path)) {
                json(exchange, 201, service.addCustomer(body(exchange))); return;
            }
            if ("POST".equals(method) && "/api/plans".equals(path)) {
                json(exchange, 201, service.addPlan(body(exchange))); return;
            }
            Matcher plan = Pattern.compile("^/api/plans/(\\d+)$").matcher(path);
            if ("PUT".equals(method) && plan.matches()) {
                json(exchange, 200, service.updatePlan(Integer.parseInt(plan.group(1)), body(exchange))); return;
            }
            if ("DELETE".equals(method) && plan.matches()) {
                service.removePlan(Integer.parseInt(plan.group(1))); json(exchange, 200, Map.of("ok", true)); return;
            }
            Matcher customer = Pattern.compile("^/api/customers/(\\d+)$").matcher(path);
            if ("PUT".equals(method) && customer.matches()) {
                json(exchange, 200, service.updateCustomer(Integer.parseInt(customer.group(1)), body(exchange))); return;
            }
            if ("DELETE".equals(method) && customer.matches()) {
                service.removeCustomer(Integer.parseInt(customer.group(1))); json(exchange, 200, Map.of("ok", true)); return;
            }
            if ("POST".equals(method) && "/api/subscriptions".equals(path)) {
                Map<String, Object> body = body(exchange);
                json(exchange, 201, service.subscribe(integer(body, "customerId"), integer(body, "planId"))); return;
            }
            Matcher usage = USAGE_PATH.matcher(path);
            if ("POST".equals(method) && usage.matches()) {
                json(exchange, 200, service.addUsage(Integer.parseInt(usage.group(1)), number(body(exchange), "gb"))); return;
            }
            if ("POST".equals(method) && "/api/complaints".equals(path)) {
                json(exchange, 201, service.addComplaint(body(exchange))); return;
            }
            Matcher complaint = COMPLAINT_PATH.matcher(path);
            if ("PATCH".equals(method) && complaint.matches()) {
                json(exchange, 200, service.updateComplaint(decode(complaint.group(1)), text(body(exchange), "status"))); return;
            }
            Matcher bill = BILL_PATH.matcher(path);
            if ("POST".equals(method) && bill.matches()) {
                json(exchange, 200, service.markInvoicePaid(decode(bill.group(1)))); return;
            }
            if ("POST".equals(method) && "/api/reports".equals(path)) { json(exchange, 202, reports.start()); return; }
            Matcher cancel = REPORT_CANCEL_PATH.matcher(path);
            if ("POST".equals(method) && cancel.matches()) { json(exchange, 200, reports.cancel(decode(cancel.group(1)))); return; }
            Matcher report = REPORT_PATH.matcher(path);
            if ("GET".equals(method) && report.matches()) {
                Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
                int version = parseInt(query.getOrDefault("version", "-1"), -1);
                long wait = Math.min(5000, Math.max(0, parseInt(query.getOrDefault("wait", "0"), 0)));
                json(exchange, 200, reports.read(decode(report.group(1)), version, wait)); return;
            }
            json(exchange, 404, Map.of("error", "API route not found."));
        } catch (CustomerNotFoundException | PlanNotFoundException | InvalidUsageException |
                 InvalidComplaintException | InvalidCustomerException | InvalidPlanException | IllegalArgumentException exception) {
            json(exchange, 400, Map.of("error", message(exception)));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            json(exchange, 503, Map.of("error", "The request was interrupted."));
        } catch (Exception exception) {
            json(exchange, 500, Map.of("error", message(exception)));
        } finally {
            exchange.close();
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        try {
            String path = URLDecoder.decode(exchange.getRequestURI().getPath(), StandardCharsets.UTF_8);
            if (path.equals("/")) path = "/index.html";
            Path file = frontendRoot.resolve(path.substring(1)).normalize();
            if (!file.startsWith(frontendRoot) || !Files.isRegularFile(file)) {
                send(exchange, 404, "Not found"); return;
            }
            String type = Files.probeContentType(file);
            if (type == null) type = "application/octet-stream";
            byte[] content = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", type + (type.startsWith("text/") ? "; charset=utf-8" : ""));
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
        } catch (RuntimeException exception) {
            send(exchange, 400, "Invalid file path");
        } finally { exchange.close(); }
    }

    private static Map<String, Object> body(HttpExchange exchange) throws IOException {
        String value = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return Json.object(Json.parse(value));
    }
    private static int integer(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof Number n)) throw new IllegalArgumentException(key + " must be a number.");
        return n.intValue();
    }
    private static double number(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof Number n)) throw new IllegalArgumentException(key + " must be a number.");
        return n.doubleValue();
    }
    private static String text(Map<String, Object> body, String key) { return body.get(key) == null ? "" : String.valueOf(body.get(key)); }
    private static int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; } }
    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private static Map<String, String> query(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(decode(parts[0]), parts.length > 1 ? decode(parts[1]) : "");
        }
        return result;
    }
    private static String message(Exception exception) { return exception.getMessage() == null ? "Request failed." : exception.getMessage(); }
    private static void json(HttpExchange exchange, int status, Object value) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        send(exchange, status, Json.stringify(value));
    }
    private static void send(HttpExchange exchange, int status, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (status == 204) { exchange.sendResponseHeaders(status, -1); return; }
        exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes);
    }
    @Override public void close() { server.stop(0); reports.close(); requestPool.shutdownNow(); }
}
