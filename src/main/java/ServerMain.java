import com.auth0.jwt.algorithms.Algorithm;
import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import encryptor.MessageEncryptor;
import org.flywaydb.core.Flyway;
import processor.IProcessor;
import processor.Processor;
import repository.ProductCategoryRepository;
import repository.ProductRepository;
import server.Server;
import service.ProductCategoryService;
import service.ProductService;
import utils.DBConnectionPool;

import java.util.Scanner;

public class ServerMain {
    private static Server currentServer = null;
    private static boolean isRunning = false;

    private final static String host = "localhost";
    private final static String port = System.getenv("DB_PORT");
    private final static String dbName = System.getenv("DB_NAME");
    private final static String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);

    private final static String user = System.getenv("DB_USER");
    private final static String password = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        printWelcomeMessage();
        migrate();
        autoStartServer();

        label:
        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "exit":
                    handleExit();
                    break label;
                case "start":
                    handleStart();
                    break;
                case "stop":
                    handleStop();
                    break;
                default:
                    System.out.println("Unknown command. Use: 'start', 'stop' or 'exit'");
                    break;
            }
        }

        System.out.println("The main branch has been completed. Application closed.");
    }

    private static void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .load();

        flyway.migrate();

        System.out.println("Flyway has been migrated.");
    }

    private static void printWelcomeMessage() {
        System.out.println("Store Server Control Panel");
        System.out.println("Available commands: 'start', 'stop', 'exit'");
    }

    private static void autoStartServer() {
        System.out.println("Automatically booting up the initial server instance...");
        bootNewServerInstance();
    }

    private static void handleStart() {
        if (isRunning) {
            System.out.println("Server is already running! Type 'stop' first if you want to restart it.");
        } else {
            System.out.println("Initializing clean environment and starting a new server instance...");
            bootNewServerInstance();
        }
    }

    private static void handleStop() {
        if (!isRunning || currentServer == null) {
            System.out.println("Server is already stopped.");
        } else {
            shutdownActiveServer();
            System.out.println("Server successfully stopped. You can type 'start' to boot up a fresh instance.");
        }
    }

    private static void handleExit() {
        if (isRunning && currentServer != null) {
            System.out.println("Stopping the active server instance before exit...");
            shutdownActiveServer();
        }
    }

    private static void bootNewServerInstance() {
        currentServer = initServer();
        currentServer.start();
        isRunning = true;
    }

    private static void shutdownActiveServer() {
        System.out.println("Initiating graceful shutdown...");
        currentServer.stop();
        currentServer = null;
        isRunning = false;
    }

    private static Server initServer() {
        DBConnectionPool dbConnectionPool = new DBConnectionPool(10, url, user, password);

        ProductCategoryRepository categoryRepository = new ProductCategoryRepository(dbConnectionPool);
        ProductRepository productRepository = new ProductRepository(dbConnectionPool);
        repository.UserRepository userRepository = new repository.UserRepository(dbConnectionPool);

        ProductCategoryService categoryService = new ProductCategoryService(categoryRepository, productRepository);
        ProductService productService = new ProductService(productRepository, categoryRepository);
        
        Algorithm jwtAlgorithm = Algorithm.HMAC256(
                System.getenv("JWT_SECRET") != null
                        ? System.getenv("JWT_SECRET")
                        : "default-secret"
        );
        service.UserService userService = new service.UserService(userRepository, jwtAlgorithm);

        IDecryptor serverDecryptor = new MessageDecryptor();
        MessageEncryptor serverEncryptor = new MessageEncryptor();
        IProcessor serverProcessor = new Processor(productService, categoryService);

        int tcpUdpPort = 10000;
        int httpPort = 8080;

        return new Server(
                5,
                serverDecryptor,
                2,
                serverEncryptor,
                3,
                serverProcessor,
                4,
                tcpUdpPort,
                httpPort,
                userService,
                productService
        );
    }
}
