package client;

import dto.request.CreateProductRequest;
import dto.request.UserRequest;
import dto.response.CreateResourceResponse;
import dto.response.JWTTokenResponse;
import dto.response.ProductResponse;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class StoreClientHTTP {
    private final String baseUrl;
    private final HttpClient client;
    private final JsonMapper mapper;
    private String token;

    public StoreClientHTTP(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        this.client = HttpClient.newHttpClient();
        this.mapper = JsonMapper.builder().build();
    }

    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public boolean login(String username, String password) {
        try {
            UserRequest req = new UserRequest(username, password);
            String body = mapper.writeValueAsString(req);

            HttpRequest request = requestBuilder("/login")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JWTTokenResponse tokenResp = mapper.readValue(response.body(), JWTTokenResponse.class);
                this.token = tokenResp.accessToken();
                System.out.println("Login successful. Token saved.");
                return true;
            } else {
                System.out.println("Login failed: " + response.statusCode() + " " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error during login: " + e.getMessage());
            return false;
        }
    }

    public boolean register(String username, String password) {
        try {
            UserRequest req = new UserRequest(username, password);
            String body = mapper.writeValueAsString(req);

            HttpRequest request = requestBuilder("/register")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("Registration successful.");
                return true;
            } else {
                System.out.println("Registration failed: " + response.statusCode() + " " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            return false;
        }
    }

    public boolean getProduct(int id) {
        try {
            HttpRequest request = requestBuilder("/products/" + id)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ProductResponse product = mapper.readValue(response.body(), ProductResponse.class);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public int createProduct(String name, int initialStock, BigDecimal price, int categoryId) {
        try {
            CreateProductRequest req = new CreateProductRequest(name, initialStock, price, categoryId);
            String body = mapper.writeValueAsString(req);

            HttpRequest request = requestBuilder("/products")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                CreateResourceResponse resp = mapper.readValue(response.body(), CreateResourceResponse.class);
                System.out.println("Product created with ID: " + resp.id());
                return resp.id();
            } else {
                System.out.println("Failed to create product: " + response.statusCode() + " " + response.body());
                return -1;
            }
        } catch (Exception e) {
            System.err.println("Error creating product: " + e.getMessage());
            return -1;
        }
    }

    public boolean updateProduct(int id, String name, int initialStock, BigDecimal price, int categoryId) {
        try {
            CreateProductRequest req = new CreateProductRequest(name, initialStock, price, categoryId);
            String body = mapper.writeValueAsString(req);

            HttpRequest request = requestBuilder("/products/" + id)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Product updated successfully.");
                return true;
            } else {
                System.out.println("Failed to update product: " + response.statusCode() + " " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error updating product: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProduct(int id) {
        try {
            HttpRequest request = requestBuilder("/products/" + id)
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                System.out.println("Product deleted successfully.");
                return true;
            } else {
                System.out.println("Failed to delete product: " + response.statusCode() + " " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error deleting product: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser() {
        try {
            HttpRequest request = requestBuilder("/user")
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                System.out.println("User deleted successfully. Token cleared.");
                this.token = null;
                return true;
            } else {
                System.out.println("Failed to delete user: " + response.statusCode() + " " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }
}
