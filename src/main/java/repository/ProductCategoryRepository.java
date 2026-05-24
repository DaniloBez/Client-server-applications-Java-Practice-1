package repository;

import entity.ProductCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProductCategoryRepository {
    private final ConcurrentHashMap<Integer, ProductCategory> categories;

    public ProductCategoryRepository() {
        categories = new ConcurrentHashMap<>();
    }

    public int create(String name) {
        ProductCategory productCategory = new ProductCategory(name);
        categories.put(productCategory.getId(), productCategory);
        return productCategory.getId();
    }

    public ProductCategory update(int id, String name) {
        ProductCategory productCategory = categories.get(id);
        productCategory.setName(name);
        return productCategory;
    }

    public ProductCategory get(int id) {
        return categories.get(id);
    }

    public ProductCategory delete(int id) {
        return categories.remove(id);
    }

    public List<ProductCategory> getAll() {
        return new ArrayList<>(categories.values());
    }
}
