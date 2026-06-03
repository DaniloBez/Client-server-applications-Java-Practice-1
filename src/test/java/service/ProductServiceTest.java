package service;

import dto.request.SearchProductsRequest;
import dto.response.PageResponse;
import entity.Product;
import entity.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.ProductCategoryRepository;
import repository.ProductRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    public void setUp() {
        testProduct = new Product(1, "Test Product", 100, new BigDecimal("50.0"), 1);
    }

    @Test
    public void shouldCreateProductSuccessfully() {
        when(categoryRepository.get(1)).thenReturn(new ProductCategory(1, "Test Category"));

        Product expectedProductToCreate = new Product(0, "New Product", 10, new BigDecimal("20.0"), 1);
        when(productRepository.create(expectedProductToCreate)).thenReturn(5);

        int id = productService.createProduct("New Product", 10, new BigDecimal("20.0"), 1);

        assertEquals(5, id);
        verify(productRepository, times(1)).create(expectedProductToCreate);
    }

    @Test
    public void shouldThrowExceptionWhenCreatingDuplicateProduct() {
        when(categoryRepository.get(1)).thenReturn(new ProductCategory(1, "Test Category"));

        SQLException sqlException = new SQLException("Duplicate key", "23505");
        when(productRepository.create(any(Product.class))).thenThrow(new RuntimeException(sqlException));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct("New Product", 10, new BigDecimal("20.0"), 1));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    public void shouldThrowExceptionWhenCreatingWithNonExistentCategory() {
        when(categoryRepository.get(99)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct("New Product", 10, new BigDecimal("20.0"), 99));

        assertTrue(exception.getMessage().contains("the group with ID 99 does not exist"));
        verify(productRepository, never()).create(any(Product.class));
    }

    @Test
    public void shouldThrowExceptionWhenCreatingWithNegativeStock() {
        when(categoryRepository.get(1)).thenReturn(new ProductCategory(1, "Test Category"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct("New Product", -5, new BigDecimal("20.0"), 1));

        assertEquals("The initial quantity cannot be negative", exception.getMessage());
    }

    @Test
    public void shouldSetPriceSuccessfully() {
        productService.setProductPrice(testProduct.id(), new BigDecimal("99.99"));

        verify(productRepository, times(1)).setProductPrice(testProduct.id(), new BigDecimal("99.99"));
    }

    @Test
    public void shouldThrowExceptionWhenSettingNegativePrice() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.setProductPrice(1, new BigDecimal("-10.0")));

        assertEquals("A price cannot be negative", exception.getMessage());
        verify(productRepository, never()).setProductPrice(anyInt(), any());
    }

    @Test
    public void shouldDeductStockSuccessfully() {
        when(productRepository.deductStock(testProduct.id(), 20)).thenReturn(true);

        productService.deductStock(testProduct.id(), 20);

        verify(productRepository, times(1)).deductStock(testProduct.id(), 20);
    }

    @Test
    public void shouldThrowExceptionWhenDeductingMoreThanInStock() {
        when(productRepository.deductStock(testProduct.id(), 150)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productService.deductStock(testProduct.id(), 150));

        assertTrue(exception.getMessage().contains("There are not enough items in stock"));
    }

    @Test
    public void shouldSearchProductsSuccessfully() {
        SearchProductsRequest request = new SearchProductsRequest(null, null, null);
        PageResponse<Product> expectedResponse = new PageResponse<>(List.of(testProduct), 1, 1, 1);

        when(productRepository.searchProducts(request)).thenReturn(expectedResponse);

        PageResponse<Product> result = productService.searchProducts(request);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(testProduct.id(), result.items().getFirst().id());

        verify(productRepository, times(1)).searchProducts(request);
    }
}