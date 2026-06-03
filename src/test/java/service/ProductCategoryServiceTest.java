package service;

import dto.request.SearchCategoriesRequest;
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
public class ProductCategoryServiceTest {
    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCategoryService categoryService;

    private ProductCategory testCategory;

    @BeforeEach
    public void setUp() {
        testCategory = new ProductCategory(1, "Test Category");
    }

    @Test
    public void shouldCreateCategorySuccessfully() {
        when(categoryRepository.create(new ProductCategory(0, "New Category"))).thenReturn(1);

        int id = categoryService.createCategory("New Category");

        assertEquals(1, id);
    }

    @Test
    public void shouldThrowExceptionWhenCreatingWithEmptyName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.createCategory("   "));

        assertEquals("The category name cannot be empty", exception.getMessage());
        verify(categoryRepository, never()).create(any());
    }

    @Test
    public void shouldThrowExceptionWhenCreatingDuplicateName() {
        SQLException sqlException = new SQLException("Duplicate key", "23505");
        when(categoryRepository.create(new ProductCategory(0, "Duplicate"))).thenThrow(new RuntimeException(sqlException));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.createCategory("Duplicate"));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    public void shouldGetCategorySuccessfully() {
        when(categoryRepository.get(1)).thenReturn(testCategory);

        ProductCategory result = categoryService.getCategory(1);

        assertNotNull(result);
        assertEquals(testCategory.name(), result.name());
    }

    @Test
    public void shouldThrowExceptionWhenCategoryNotFound() {
        when(categoryRepository.get(99)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.getCategory(99));

        assertEquals("The category with ID 99 was not found", exception.getMessage());
    }

    @Test
    public void shouldDeleteCategorySuccessfully() {
        when(productRepository.searchProducts(any(SearchProductsRequest.class))).thenReturn(new PageResponse<>(List.of(), 0, 0, 1));
        when(categoryRepository.delete(1)).thenReturn(true);

        boolean result = categoryService.deleteCategory(1);

        assertTrue(result);
        verify(categoryRepository, times(1)).delete(1);
    }

    @Test
    public void shouldThrowExceptionWhenDeletingCategoryWithProducts() {
        Product dummyProduct = new Product(1, "Prod", 10, new BigDecimal("10.0"), 1);

        when(productRepository.searchProducts(any(SearchProductsRequest.class)))
                .thenReturn(new PageResponse<>(List.of(dummyProduct), 1, 1, 1));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> categoryService.deleteCategory(1));

        assertEquals("You cannot delete the group: it still contains items", exception.getMessage());
        verify(categoryRepository, never()).delete(anyInt());
    }

    @Test
    public void shouldSearchCategoriesSuccessfully() {
        SearchCategoriesRequest request = new SearchCategoriesRequest(null, null, null);
        PageResponse<ProductCategory> expectedResponse = new PageResponse<>(List.of(testCategory), 1, 1, 1);

        when(categoryRepository.searchCategories(request)).thenReturn(expectedResponse);

        PageResponse<ProductCategory> result = categoryService.searchCategories(request);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals("Test Category", result.items().getFirst().name());
        verify(categoryRepository, times(1)).searchCategories(request);
    }
}
