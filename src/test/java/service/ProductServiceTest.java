package service;

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
        testProduct = new Product("Test Product", 100, new BigDecimal("50.0"), 1);
    }

    @Test
    public void shouldCreateProductSuccessfully() {
        when(categoryRepository.get(1)).thenReturn(new ProductCategory("Test Category"));
        when(productRepository.create("New Product", 10, new BigDecimal("20.0"), 1)).thenReturn(5);

        int id = productService.createProduct("New Product", 10, new BigDecimal("20.0"), 1);

        assertEquals(5, id);
        verify(productRepository, times(1)).create(anyString(), anyInt(), any(), anyInt());
    }

    @Test
    public void shouldThrowExceptionWhenCreatingWithNonExistentCategory() {
        when(categoryRepository.get(99)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct("New Product", 10, new BigDecimal("20.0"), 99));

        assertTrue(exception.getMessage().contains("the group with ID 99 does not exist"));
        verify(productRepository, never()).create(anyString(), anyInt(), any(), anyInt());
    }

    @Test
    public void shouldThrowExceptionWhenCreatingWithNegativeStock() {
        when(categoryRepository.get(1)).thenReturn(new ProductCategory("Test Category"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct("New Product", -5, new BigDecimal("20.0"), 1));

        assertEquals("The initial quantity cannot be negative", exception.getMessage());
    }

    @Test
    public void shouldSetPriceSuccessfully() {
        when(productRepository.get(testProduct.getId())).thenReturn(testProduct);

        productService.setProductPrice(testProduct.getId(), new BigDecimal("99.99"));

        assertEquals(new BigDecimal("99.99"), testProduct.getPrice().get());
    }

    @Test
    public void shouldThrowExceptionWhenSettingNegativePrice() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.setProductPrice(1, new BigDecimal("-10.0")));

        assertEquals("A price cannot be negative", exception.getMessage());
        verify(productRepository, never()).get(anyInt());
    }

    @Test
    public void shouldDeductStockSuccessfully() {
        when(productRepository.get(testProduct.getId())).thenReturn(testProduct);

        productService.deductStock(testProduct.getId(), 20);

        assertEquals(80, testProduct.getCountInStock().get());
    }

    @Test
    public void shouldThrowExceptionWhenDeductingMoreThanInStock() {
        when(productRepository.get(testProduct.getId())).thenReturn(testProduct);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productService.deductStock(testProduct.getId(), 150));

        assertTrue(exception.getMessage().contains("There are not enough items in stock"));
        assertEquals(100, testProduct.getCountInStock().get());
    }

    @Test
    public void shouldGetProductsByCategorySuccessfully() {
        int categoryId = 1;
        when(categoryRepository.get(categoryId)).thenReturn(new ProductCategory("Test Category"));
        when(productRepository.getAllByCategoryId(categoryId)).thenReturn(List.of(testProduct));

        List<Product> products = productService.getProductsByCategory(categoryId);

        assertNotNull(products);
        assertEquals(1, products.size());
        assertEquals(testProduct.getId(), products.getFirst().getId());

        verify(categoryRepository, times(1)).get(categoryId);
        verify(productRepository, times(1)).getAllByCategoryId(categoryId);
    }

    @Test
    public void shouldThrowExceptionWhenGettingProductsForNonExistentCategory() {
        int categoryId = 99;
        when(categoryRepository.get(categoryId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productService.getProductsByCategory(categoryId));

        assertTrue(exception.getMessage().contains("the group with ID 99 does not exist"));

        verify(categoryRepository, times(1)).get(categoryId);
        verify(productRepository, never()).getAllByCategoryId(anyInt());
    }
}
