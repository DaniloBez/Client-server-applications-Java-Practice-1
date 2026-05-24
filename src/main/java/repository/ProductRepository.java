package repository;

import entity.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProductRepository {
    private final ConcurrentHashMap<Integer, Product> products;

    public ProductRepository() {
        products = new ConcurrentHashMap<>();
    }

    public int create(String name, int countInStock, BigDecimal price, int productCategoryId) {
        Product product = new Product(name, countInStock, price, productCategoryId);
        products.put(product.getId(), product);
        return product.getId();
    }

    public Product get(int id) {
        return products.get(id);
    }

    public Product delete(int id) {
        return products.remove(id);
    }

    public List<Product> getAllByCategoryId(int categoryId) {
        return new ArrayList<>(
                products.values()
                        .stream()
                        .filter(p -> p.getProductCategoryId().get() == categoryId)
                        .toList()
        );
    }

    public boolean hasProductsInCategory(int categoryId) {
        return products.values()
                .stream()
                .anyMatch(p -> p.getProductCategoryId().get() == categoryId);
    }
}
