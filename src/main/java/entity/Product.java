package entity;


import lombok.Getter;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class Product {
    private static final AtomicInteger counter = new AtomicInteger(0);

    private final int id;
    private final AtomicReference<String> name;
    private final AtomicInteger countInStock;
    private final AtomicReference<BigDecimal> price;
    private final AtomicInteger productCategoryId;

    public Product(String name, int countInStock, BigDecimal price, int productCategoryId){
        this.id = counter.incrementAndGet();
        this.name = new AtomicReference<>(name);
        this.countInStock = new AtomicInteger(countInStock);
        this.price = new AtomicReference<>(price);
        this.productCategoryId = new AtomicInteger(productCategoryId);
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void setPrice(BigDecimal price) {
        this.price.set(price);
    }

    public void setProductCategoryId(int productCategoryId) {
        this.productCategoryId.set(productCategoryId);
    }

    public void addStock(int amount) {
        if(amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");

        this.countInStock.addAndGet(amount);
    }

    public boolean deductStock(int amount) {
        if(amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");

        while(true) {
            int current = this.countInStock.get();

            if (current < amount)
                return false;

            if(countInStock.compareAndSet(current, current - amount))
                return true;
        }
    }
}
