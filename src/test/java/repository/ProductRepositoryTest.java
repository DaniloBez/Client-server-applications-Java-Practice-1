package repository;

import dto.request.ProductFilterDTO;
import dto.request.SearchProductsRequest;
import dto.request.SortDTO;
import dto.response.PageResponse;
import entity.Product;
import entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class ProductRepositoryTest extends BaseRepositoryTest {

    @Test
    public void shouldFindById() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        Product product = productRepository.get(id);

        assertNotNull(product);
        assertEquals(id, product.id());
        assertEquals("product1", product.name());
        assertEquals(10, product.countInStock());
        assertEquals(new BigDecimal("100.0"), product.price());
        assertEquals(categoryId, product.productCategoryId());
    }

    @Test
    public void shouldDeleteById() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );


        assertTrue(productRepository.delete(id));
        assertNull(productRepository.get(id));
        assertFalse(productRepository.delete(id));
    }

    @Test
    public void shouldCheckPresenceInCategory() {
        int categoryId1 = productCategoryRepository.create(new ProductCategory(0, "Category for Product1"));
        int categoryId2 = productCategoryRepository.create(new ProductCategory(0, "Category for Product2"));
        productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId1
                )
        );

        productRepository.create(
                new Product(
                        0,
                        "product2",
                        10,
                        new BigDecimal(100),
                        categoryId1
                )
        );

        productRepository.create(
                new Product(
                        0,
                        "product3",
                        10,
                        new BigDecimal(100),
                        categoryId2
                )
        );

        assertFalse(productRepository.searchProducts(new SearchProductsRequest(new ProductFilterDTO(null, null, null, categoryId1), null, null)).items().isEmpty());
        assertFalse(productRepository.searchProducts(new SearchProductsRequest(new ProductFilterDTO(null, null, null, categoryId2), null, null)).items().isEmpty());
        assertTrue(productRepository.searchProducts(new SearchProductsRequest(new ProductFilterDTO(null, null, null, categoryId1 + categoryId2), null, null)).items().isEmpty());
    }

    @Test
    public void shouldSearchAndFilterProducts() {
        int cat1 = productCategoryRepository.create(new ProductCategory(0, "Laptops"));
        int cat2 = productCategoryRepository.create(new ProductCategory(0, "Phones"));

        productRepository.create(new Product(0, "MacBook Pro", 10, new BigDecimal("2000.0"), cat1));
        productRepository.create(new Product(0, "MacBook Air", 15, new BigDecimal("1000.0"), cat1));
        productRepository.create(new Product(0, "iPhone 15", 50, new BigDecimal("1000.0"), cat2));

        SearchProductsRequest req1 = new SearchProductsRequest(new ProductFilterDTO("MacBook", null, null, cat1), null, null);
        PageResponse<Product> res1 = productRepository.searchProducts(req1);
        assertEquals(2, res1.totalElements());

        SearchProductsRequest req2 = new SearchProductsRequest(new ProductFilterDTO(null, new BigDecimal("1500.0"), null, null), null, null);
        PageResponse<Product> res2 = productRepository.searchProducts(req2);
        assertEquals(1, res2.totalElements());
        assertEquals("MacBook Pro", res2.items().getFirst().name());

        SearchProductsRequest req3 = new SearchProductsRequest(null, null, new SortDTO("price", "DESC"));
        PageResponse<Product> res3 = productRepository.searchProducts(req3);
        assertEquals(3, res3.totalElements());
        assertEquals("MacBook Pro", res3.items().getFirst().name());
    }

    @Test
    public void shouldAddStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertEquals(10, productRepository.get(id).countInStock());

        productRepository.addStock(id, 10);
        assertEquals(20, productRepository.get(id).countInStock());

        productRepository.addStock(id, 30);
        assertEquals(50, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldThrowErrorAndNotAddNegativeStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertThatThrownBy(() -> productRepository.addStock(id,-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        assertEquals(10, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldDeductStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertTrue(productRepository.deductStock(id,5));
        assertEquals(5, productRepository.get(id).countInStock());

        assertTrue(productRepository.deductStock(id,5));
        assertEquals(0, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldThrowErrorAndNotDeductNegativeStock() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertThatThrownBy(() -> productRepository.deductStock(id,-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than 0");

        assertEquals(10, productRepository.get(id).countInStock());
    }

    @Test
    public void shouldNotDeductIfLessThenAmount() {
        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));
        int id = productRepository.create(
                new Product(
                        0,
                        "product1",
                        10,
                        new BigDecimal(100),
                        categoryId
                )
        );

        assertFalse(productRepository.deductStock(id,15));
        assertEquals(10, productRepository.get(id).countInStock());
    }
}
