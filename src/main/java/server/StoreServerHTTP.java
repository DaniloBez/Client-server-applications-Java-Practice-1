package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dto.request.CreateProductRequest;
import dto.request.UserRequest;
import dto.response.CreateResourceResponse;
import dto.response.ErrorResponse;
import dto.response.JWTTokenResponse;
import dto.response.ProductResponse;
import entity.Product;
import entity.User;
import lombok.extern.slf4j.Slf4j;
import service.ProductService;
import service.UserService;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

@Slf4j
public class StoreServerHTTP {
    private final int port;
    private final UserService userService;
    private final ProductService productService;

    private final JsonMapper mapper = JsonMapper.builder().build();

    private HttpServer httpServer;

    public StoreServerHTTP(int port, UserService userService, ProductService productService) {
        this.port = port;

        this.userService = userService;
        this.productService = productService;
    }

    public void start() {
        try {
            log.info("Starting HTTP server on port {}", port);
            httpServer = HttpServer.create(new InetSocketAddress(port), 1000);
            httpServer.setExecutor(Executors.newFixedThreadPool(50));
            httpServer.start();
            init();
        } catch (IOException e) {
            log.error("Error starting HTTP server on port {}", port, e);
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        log.info("Stopping HTTP server on port {}", port);
        if (httpServer != null)
            httpServer.stop(1);
    }

    private void init() {
        httpServer.createContext("/login", this::loginHandler);
        httpServer.createContext("/register", this::registerHandler);
        httpServer.createContext("/products", this::productsHandler);
        httpServer.createContext("/user", this::userHandler);
        httpServer.createContext("/", this::pageNotFoundHandler);
    }

    private void loginHandler(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);
        UserRequest request = mapper.readValue(requestBody, UserRequest.class);

        log.debug("Processing login request for user: {}", request.username());

        String token = userService.login(request.username(), request.password());

        if (token == null) {
            log.warn("Failed login attempt for user: {}", request.username());
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        log.info("User logged in successfully: {}", request.username());

        JWTTokenResponse response = new JWTTokenResponse(token);
        byte[] responseBytes = mapper.writeValueAsBytes(response);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try(OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void registerHandler(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);
        UserRequest request = mapper.readValue(requestBody, UserRequest.class);

        log.debug("Processing registration request for user: {}", request.username());

        try {
            userService.register(request.username(), request.password());
            log.info("New user registered successfully: {}", request.username());
            exchange.sendResponseHeaders(201, -1);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to register user {}: {}", request.username(), e.getMessage());

            byte[] errorBytes = mapper.writeValueAsBytes(new ErrorResponse("Conflict", e.getMessage()));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(409, errorBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        } finally {
            exchange.close();
        }
    }

    private void productsHandler(HttpExchange exchange) throws IOException {
        if (!verify(exchange)) {
            return;
        }

        switch (exchange.getRequestMethod()) {
            case "GET" -> getProductsHandler(exchange);
            case "PUT" -> putProductsHandler(exchange);
            case "POST" -> postProductsHandler(exchange);
            case "DELETE" -> deleteProductsHandler(exchange);
            default -> {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }
    }

    private boolean verify(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return false;
        }

        boolean isValid = userService.verify(authHeader.substring(7));
        if (!isValid) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        }
        return isValid;
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] errorBytes = mapper.writeValueAsBytes(new ErrorResponse("Error", message));
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, errorBytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(errorBytes); }
    }

    private void getProductsHandler(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length != 3) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            int id = Integer.parseInt(parts[2]);
            log.debug("Fetching product with ID: {}", id);
            Product product = productService.getProduct(id);

            ProductResponse response = new ProductResponse(
                    product.id(),
                    product.name(),
                    product.countInStock(),
                    product.price(),
                    product.productCategoryId()
            );

            byte[] respBytes = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, respBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("not found") ? 404 : 400;
            log.warn("Failed to fetch product: {}", e.getMessage());
            sendError(exchange, status, e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching product", e);
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

    private void putProductsHandler(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (!path.equals("/products") && !path.equals("/products/")) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }


            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            CreateProductRequest req = mapper.readValue(bodyBytes, CreateProductRequest.class);

            log.debug("Processing request to create new product: {}", req.name());

            int id = productService.createProduct(req.name(), req.initialStock(), req.price(), req.categoryId());
            log.info("Created new product '{}' with ID {}", req.name(), id);
            CreateResourceResponse response = new CreateResourceResponse(id);

            byte[] respBytes = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, respBytes.length);

            try(OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("already exists") ? 409 : 400;
            log.warn("Failed to create product: {}", e.getMessage());
            sendError(exchange, status, e.getMessage());
        } catch (Exception e) {
            log.error("Error creating product", e);
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

    private void postProductsHandler(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length != 3) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            int id = Integer.parseInt(parts[2]);
            log.debug("Processing request to update product with ID: {}", id);
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();

            CreateProductRequest request = mapper.readValue(bodyBytes, CreateProductRequest.class);
            productService.updateProduct(
                    id,
                    request.name(),
                    request.initialStock(),
                    request.price(),
                    request.categoryId()
            );

            log.info("Updated product with ID {}", id);
            exchange.sendResponseHeaders(200, -1);
        } catch (IllegalArgumentException e) {
            int status = 400;
            if (e.getMessage().contains("not found"))
                status = 404;
            else if (e.getMessage().contains("already exists"))
                status = 409;

            log.warn("Failed to update product: {}", e.getMessage());
            sendError(exchange, status, e.getMessage());
        } catch (Exception e) {
            log.error("Error updating product", e);
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

    private void deleteProductsHandler(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length != 3) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            int id = Integer.parseInt(parts[2]);
            log.debug("Processing request to delete product with ID: {}", id);
            boolean deleted = productService.deleteProduct(id);
            if (deleted) {
                log.info("Deleted product with ID {}", id);
                exchange.sendResponseHeaders(204, -1);
            } else {
                log.warn("Attempted to delete non-existent product with ID {}", id);
                exchange.sendResponseHeaders(404, -1);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid product ID format for deletion");
            exchange.sendResponseHeaders(400, -1);
        } catch (Exception e) {
            log.error("Error deleting product", e);
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

    private void userHandler(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("DELETE")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        String token = authHeader.substring(7);
        String username = userService.verifyAndGetUsername(token);

        if (username == null) {
            log.warn("Unauthorized attempt to delete user (invalid token)");
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        try {
            log.debug("Processing request to delete user account: {}", username);
            User user = userService.getByUsername(username);
            userService.deleteUser(user.id());
            log.info("User deleted their account: {}", username);
            exchange.sendResponseHeaders(204, -1);
        } catch (IllegalArgumentException e) {
            log.warn("Attempted to delete non-existent user account: {}", username);
            exchange.sendResponseHeaders(404, -1);
        } finally {
            exchange.close();
        }
    }

    private void pageNotFoundHandler(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, 0);
        exchange.close();
    }
}
