package repository;

import dto.request.CategoryFilterDTO;
import dto.request.PaginationDTO;
import dto.request.SearchCategoriesRequest;
import dto.request.SortDTO;
import dto.response.PageResponse;
import entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class ProductCategoryRepositoryTest extends BaseRepositoryTest {

    @Test
    public void shouldFindById() {
        int id = productCategoryRepository.create(new ProductCategory(0, "Category1"));

        ProductCategory productCategory = productCategoryRepository.get(id);

        assertNotNull(productCategory);
        assertEquals(id, productCategory.id());
        assertEquals("Category1", productCategory.name());
    }

    @Test
    public void shouldDeleteById() {
        int id = productCategoryRepository.create(new ProductCategory(0, "CategoryToDelete"));

        boolean isDeleted = productCategoryRepository.delete(id);
        assertTrue(isDeleted);

        assertNull(productCategoryRepository.get(id));

        boolean isDeletedAgain = productCategoryRepository.delete(id);
        assertFalse(isDeletedAgain);
    }

    @Test
    public void shouldGetAllWithEmptyRequest() {
        productCategoryRepository.create(new ProductCategory(0, "Category1"));
        productCategoryRepository.create(new ProductCategory(0, "Category2"));
        productCategoryRepository.create(new ProductCategory(0, "Category3"));

        PageResponse<ProductCategory> response = productCategoryRepository.searchCategories(new SearchCategoriesRequest(null, null, null));

        assertEquals(3, response.totalElements());
        assertEquals(3, response.items().size());
        assertEquals(1, response.totalPages());
    }

    @Test
    public void shouldPaginateCategories() {
        for (int i = 1; i <= 5; i++) {
            productCategoryRepository.create(new ProductCategory(0, "Cat " + i));
        }

        SearchCategoriesRequest request = new SearchCategoriesRequest(
                null,
                new PaginationDTO(2, 2),
                null
        );

        PageResponse<ProductCategory> response = productCategoryRepository.searchCategories(request);

        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(2, response.items().size());
        assertEquals("Cat 3", response.items().get(0).name());
        assertEquals("Cat 4", response.items().get(1).name());
    }

    @Test
    public void shouldFilterAndSortCategories() {
        productCategoryRepository.create(new ProductCategory(0, "Apple"));
        productCategoryRepository.create(new ProductCategory(0, "Banana"));
        productCategoryRepository.create(new ProductCategory(0, "Apricot"));

        SearchCategoriesRequest request = new SearchCategoriesRequest(
                new CategoryFilterDTO("Ap"),
                null,
                new SortDTO("name", "ASC")
        );

        PageResponse<ProductCategory> response = productCategoryRepository.searchCategories(request);

        assertEquals(2, response.totalElements());
        assertEquals("Apple", response.items().get(0).name());
        assertEquals("Apricot", response.items().get(1).name());
    }

    @Test
    public void shouldFailToCreateDuplicateName() {
        productCategoryRepository.create(new ProductCategory(0, "UniqueName"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> productCategoryRepository.create(new ProductCategory(0, "UniqueName")));

        Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertInstanceOf(SQLException.class, cause);

        assertEquals("23505", ((SQLException) cause).getSQLState(),
                "SQL state should match PostgreSQL unique violation code");
    }
}
