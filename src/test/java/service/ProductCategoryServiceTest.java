package service;

import entity.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.ProductCategoryRepository;
import repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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
        testCategory = new ProductCategory("Test Category");
    }

    @Test
    public void shouldCreateCategorySuccessfully() {
        when(categoryRepository.create("New Category")).thenReturn(1);

        int id = categoryService.createCategory("New Category");

        assertEquals(1, id);
    }

    @Test
    public void shouldThrowExceptionWhenCreatingWithEmptyName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.createCategory("   "));

        assertEquals("The category name cannot be empty", exception.getMessage());
        verify(categoryRepository, never()).create(anyString());
    }

    @Test
    public void shouldGetCategorySuccessfully() {
        when(categoryRepository.get(1)).thenReturn(testCategory);

        ProductCategory result = categoryService.getCategory(1);

        assertNotNull(result);
        assertEquals(testCategory.getName().get(), result.getName().get());
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
        when(categoryRepository.get(1)).thenReturn(testCategory);
        when(productRepository.hasProductsInCategory(1)).thenReturn(false);
        when(categoryRepository.delete(1)).thenReturn(testCategory);

        boolean result = categoryService.deleteCategory(1);

        assertTrue(result);
        verify(categoryRepository, times(1)).delete(1);
    }

    @Test
    public void shouldThrowExceptionWhenDeletingCategoryWithProducts() {
        when(categoryRepository.get(1)).thenReturn(testCategory);
        when(productRepository.hasProductsInCategory(1)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> categoryService.deleteCategory(1));

        assertEquals("You cannot delete the group: it still contains items", exception.getMessage());
        verify(categoryRepository, never()).delete(anyInt());
    }
}
