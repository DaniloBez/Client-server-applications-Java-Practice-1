package entity;


import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class ProductCategory {
    private static final AtomicInteger counter =  new AtomicInteger(0);

    private final int id;
    private final AtomicReference<String> name;

    public ProductCategory(String name) {
        this.id = counter.incrementAndGet();
        this.name = new AtomicReference<>(name);
    }

    public void setName(String name) {
        this.name.set(name);
    }
}
