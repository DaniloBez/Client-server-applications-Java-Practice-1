package repository;

import entity.Product;
import entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ProductRepositoryMultithreadingTest extends ProductRepositoryTest {
    @Test
    public void soldOutTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));

        int id = productRepository.create(
                new Product(
                        0,
                        "Product",
                        100,
                        new BigDecimal(10),
                        categoryId
                )
        );

        AtomicInteger numberOfSold = new AtomicInteger(0);
        AtomicInteger blocked = new AtomicInteger(0);

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        boolean isSuccessful = productRepository.deductStock(id,10);
                        if (isSuccessful)
                            numberOfSold.incrementAndGet();
                        else
                            blocked.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();

            assertEquals(10, numberOfSold.get());
            assertEquals(990, blocked.get());
            assertEquals(0, productRepository.get(id).countInStock());
        }
    }

    @Test
    public void addAndDeductTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));

        int id = productRepository.create(
                new Product(
                        0,
                        "Product",
                        10_000,
                        new BigDecimal(10),
                        categoryId
                )
        );

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

            for (int i = 0; i < numberOfThreads; i++) {
                final int finalI = i;
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        if (finalI % 2 == 0)
                            productRepository.deductStock(id,5);
                        else
                            productRepository.addStock(id,5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();

            assertEquals(10_000, productRepository.get(id).countInStock());
        }
    }

    @Test
    public void concurrentUpdateTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));

        int id = productRepository.create(
                new Product(
                        0,
                        "Product",
                        10_000,
                        new BigDecimal(10),
                        categoryId
                )
        );

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

            for (int i = 0; i < numberOfThreads; i++) {
                final int finalI = i;
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        productRepository.setProductPrice(id, new BigDecimal(finalI));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();

            BigDecimal price = productRepository.get(id).price();
            assertNotNull(price);
            assertTrue(price.compareTo(new BigDecimal(0)) > 0 &&  price.compareTo(new BigDecimal(1_001)) < 0);
        }
    }

    @Test
    public void shouldCreateCorrectly() throws InterruptedException {
        int numberOfThreads = 10_000;
        Set<Integer> generatedIds = ConcurrentHashMap.newKeySet();

        List<Integer> categoryIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int id = productCategoryRepository.create(new ProductCategory(0, "Category for Product" + i));
            categoryIds.add(id);
        }


        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

            for(int i = 0; i < numberOfThreads; i++ ) {
                int finalI = i;
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        int id = productRepository.create(
                                new Product(
                                        0,
                                        "Product" + finalI,
                                        100,
                                        new BigDecimal(15),
                                        categoryIds.get(finalI % 5)
                                )
                        );
                        generatedIds.add(id);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();

            assertEquals(numberOfThreads, generatedIds.size());
        }
    }

    @Test
    public void shouldWorkReadWriteStreamTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        List<Integer> categoryIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int id = productCategoryRepository.create(new ProductCategory(0, "Category for Product" + i));
            categoryIds.add(id);
        }

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

            for(int i = 0; i < numberOfThreads; i++ ) {
                int finalI = i;

                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        int repeat = 20;
                        int counter = 0;
                        if (finalI % 3 == 0) {
                            while (counter < repeat) {
                                int id = productRepository.create(
                                        new Product(
                                                0,
                                                "Prod_" + finalI + "_" + counter,
                                                10,
                                                new BigDecimal(1),
                                                categoryIds.get(finalI % 10)
                                        )
                                );
                                counter++;
                            }
                        }
                        else if (finalI % 3 == 1) {
                            while (counter < repeat) {
                                productRepository.delete(finalI + counter);
                                counter++;
                            }
                        }
                        else {
                            while (counter < repeat) {
                                productRepository.getAllByCategoryId(categoryIds.get(finalI % 10));
                                counter++;
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();
        }
    }

    @Test
    public void shouldWorkConcurrentDeleteTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int categoryId = productCategoryRepository.create(new ProductCategory(0, "Category for Product"));

        int id = productRepository.create(
                new Product(
                        0,
                        "Product",
                        100,
                        new BigDecimal(50),
                        categoryId
                )
        );

        AtomicInteger successfulDeletions = new AtomicInteger(0);
        AtomicInteger failedDeletions = new AtomicInteger(0);

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

            for(int i = 0; i < numberOfThreads; i++ ) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        boolean success = productRepository.delete(id);

                        if (success)
                            successfulDeletions.incrementAndGet();
                        else
                            failedDeletions.incrementAndGet();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();

            assertEquals(1, successfulDeletions.get());
            assertEquals(numberOfThreads - 1, failedDeletions.get());
            assertNull(productRepository.get(id));
        }
    }
}
