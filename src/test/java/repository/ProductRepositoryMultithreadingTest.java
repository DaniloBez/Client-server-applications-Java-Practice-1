package repository;

import entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ProductRepositoryMultithreadingTest {
    private ProductRepository productRepository;

    @BeforeEach
    public void setup() {
        productRepository = new ProductRepository();
    }

    @Test
    public void soldOutTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int id = productRepository.create(
                "Product",
                100,
                new BigDecimal(10),
                1
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

                        boolean isSuccessful = productRepository.get(id).deductStock(10);
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
            assertEquals(0, productRepository.get(id).getCountInStock().get());
        }
    }

    @Test
    public void addAndDeductTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int id = productRepository.create(
                "Product",
                10_000,
                new BigDecimal(10),
                1
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
                            productRepository.get(id).deductStock(5);
                        else
                            productRepository.get(id).addStock(5);
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

            assertEquals(10_000, productRepository.get(id).getCountInStock().get());
        }
    }

    @Test
    public void concurrentUpdateTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int id = productRepository.create(
                "Product",
                10_000,
                new BigDecimal(10),
                1
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

                        productRepository.get(id).setPrice(new BigDecimal(finalI));
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

            BigDecimal price = productRepository.get(id).getPrice().get();
            assertNotNull(price);
            assertTrue(price.compareTo(new BigDecimal(0)) > 0 &&  price.compareTo(new BigDecimal(1_001)) < 0);
        }
    }

    @Test
    public void shouldCreateCorrectly() throws InterruptedException {
        int numberOfThreads = 10_000;
        Set<Integer> generatedIds = ConcurrentHashMap.newKeySet();

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
                                "Product" + finalI,
                                100,
                                new BigDecimal(15),
                                finalI % 5
                        );
                        generatedIds.add(id);
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

            assertEquals(numberOfThreads, generatedIds.size());
        }
    }

    @Test
    public void shouldWorkReadWriteStreamTest() throws InterruptedException {
        int numberOfThreads = 1_000;

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
                                productRepository.create("Prod", 10, new BigDecimal(1), finalI % 10);
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
                                productRepository.getAllByCategoryId(finalI % 10);
                                productRepository.hasProductsInCategory(finalI % 10);
                                counter++;
                            }
                        }
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
        }
    }

    @Test
    public void shouldWorkConcurrentDeleteTest() throws InterruptedException {
        int numberOfThreads = 1_000;

        int id = productRepository.create("Product", 100, new BigDecimal(50), 1);

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

                        Product deletedProduct = productRepository.delete(id);

                        if (deletedProduct != null)
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
