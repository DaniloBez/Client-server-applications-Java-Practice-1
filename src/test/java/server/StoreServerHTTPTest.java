package server;

import dto.request.CreateProductRequest;
import dto.request.UserRequest;
import entity.Product;
import entity.User;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.ProductService;
import service.UserService;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoreServerHTTPTest {

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    private StoreServerHTTP server;

    @BeforeEach
    public void setUp() {
        server = new StoreServerHTTP(8081, userService, productService);
        server.start();
        RestAssured.baseURI = "http://localhost:8081";
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void shouldLoginSuccessfully() {
        when(userService.login("testuser", "testpass")).thenReturn("mocked-jwt-token");

        UserRequest req = new UserRequest("testuser", "testpass");

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("access_token", equalTo("mocked-jwt-token"));
    }

    @Test
    public void shouldFailLoginWithUnauthorized() {
        when(userService.login("testuser", "wrongpass")).thenReturn(null);

        UserRequest req = new UserRequest("testuser", "wrongpass");

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/login")
        .then()
            .statusCode(401);
    }

    @Test
    public void shouldRegisterSuccessfully() {
        UserRequest req = new UserRequest("newuser", "newpass");

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/register")
        .then()
            .statusCode(201);

        verify(userService, times(1)).register("newuser", "newpass");
    }

    @Test
    public void shouldFailRegisterIfConflict() {
        doThrow(new IllegalArgumentException("already exists"))
                .when(userService).register("existinguser", "pass");

        UserRequest req = new UserRequest("existinguser", "pass");

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/register")
        .then()
            .statusCode(409)
            .body("errorMessage", equalTo("already exists"));
    }

    @Test
    public void shouldGetProductSuccessfully() {
        when(userService.verify("valid-token")).thenReturn(true);
        Product mockProduct = new Product(1, "Laptop", 10, new BigDecimal("999.99"), 2);
        when(productService.getProduct(1)).thenReturn(mockProduct);

        given()
            .header("Authorization", "Bearer valid-token")
        .when()
            .get("/products/1")
        .then()
            .statusCode(200)
            .body("name", equalTo("Laptop"))
            .body("countInStock", equalTo(10));
    }

    @Test
    public void shouldCreateProductSuccessfully() {
        when(userService.verify("valid-token")).thenReturn(true);
        when(productService.createProduct("Mouse", 50, new BigDecimal("25.0"), 3)).thenReturn(5);

        CreateProductRequest req = new CreateProductRequest("Mouse", 50, new BigDecimal("25.0"), 3);

        given()
            .header("Authorization", "Bearer valid-token")
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .put("/products")
        .then()
            .statusCode(201)
            .body("id", equalTo(5));
    }

    @Test
    public void shouldUpdateProductSuccessfully() {
        when(userService.verify("valid-token")).thenReturn(true);

        CreateProductRequest req = new CreateProductRequest("Updated Mouse", 40, new BigDecimal("20.0"), 3);

        given()
            .header("Authorization", "Bearer valid-token")
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/products/5")
        .then()
            .statusCode(200);

        verify(productService, times(1)).updateProduct(5, "Updated Mouse", 40, new BigDecimal("20.0"), 3);
    }

    @Test
    public void shouldDeleteProductSuccessfully() {
        when(userService.verify("valid-token")).thenReturn(true);
        when(productService.deleteProduct(10)).thenReturn(true);

        given()
            .header("Authorization", "Bearer valid-token")
        .when()
            .delete("/products/10")
        .then()
            .statusCode(204);
    }

    @Test
    public void shouldDeleteUserSuccessfully() {
        when(userService.verifyAndGetUsername("valid-token")).thenReturn("testuser");
        User mockUser = new User(1, "testuser", "hashed");
        when(userService.getByUsername("testuser")).thenReturn(mockUser);

        given()
            .header("Authorization", "Bearer valid-token")
        .when()
            .delete("/user")
        .then()
            .statusCode(204);

        verify(userService, times(1)).deleteUser(1);
    }

    @Test
    public void shouldFailToAccessProductsWithoutToken() {
        given()
        .when()
            .get("/products/1")
        .then()
            .statusCode(401);
    }
}
