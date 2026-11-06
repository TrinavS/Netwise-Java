package isp;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        Path frontend = Path.of(System.getProperty("isp.frontend", "frontend"));
        try {
            JdbcStore database = new JdbcStore();
            database.checkConnection();
            IspHttpServer server = new IspHttpServer(new IspService(database), host, port, frontend);
            Runtime.getRuntime().addShutdownHook(new Thread(server::close, "isp-shutdown"));
            server.start();
            System.out.println("Netwise ISP portal is ready at http://" + host + ":" + server.getPort());
            System.out.println("Project records are persisted in PostgreSQL. Stop with Ctrl+C.");
            new CountDownLatch(1).await();
        } catch (Exception exception) {
            System.err.println("Could not start ISP portal: " + exception.getMessage());
            System.err.println("Check PostgreSQL is running, the isp_project database exists, and ISP_DB_PASSWORD is set.");
            System.err.println("Start this application from the project root so the frontend folder is available.");
        }
    }
}
