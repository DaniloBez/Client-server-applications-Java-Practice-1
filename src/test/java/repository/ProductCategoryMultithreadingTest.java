package repository;

import dto.request.SearchCategoriesRequest;
import entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ProductCategoryMultithreadingTest extends BaseRepositoryTest {
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

                       generatedIds.add(
                               productCategoryRepository.create(
                                       new ProductCategory(
                                               0,
                                               "Category" + finalI
                                       )
                               )
                       );
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
    public void shouldWorkReadWriteTest() throws InterruptedException {
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

                        int repeat = 30;

                        int counter = 0;
                        if (finalI % 3 == 0) {
                            while (counter < repeat) {
                                productCategoryRepository.create(
                                        new ProductCategory(
                                                0,
                                                "Category_" + finalI + "_" + counter
                                        )
                                );
                                counter++;
                            }
                        }
                        else if (finalI % 3 == 1) {
                            while (counter < repeat) {
                                productCategoryRepository.get(finalI + counter);
                                counter++;
                            }
                        }
                        else {
                            while (counter < repeat) {
                                productCategoryRepository.searchCategories(new SearchCategoriesRequest(null, null, null));
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
        int numberOfThreads = 10_000;

        final int id = productCategoryRepository.create(
                new ProductCategory(
                        0,
                        "Category"
                )
        );

        AtomicInteger deletions = new AtomicInteger(0);
        AtomicInteger blockedDeletions = new AtomicInteger(0);

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
            for(int i = 0; i < numberOfThreads; i++ ) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        boolean success = productCategoryRepository.delete(id);

                        if (success)
                            deletions.incrementAndGet();
                        else
                            blockedDeletions.incrementAndGet();
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

            assertEquals(1, deletions.get());
            assertEquals(numberOfThreads - 1, blockedDeletions.get());
        }
    }

    @Test
    public void shouldWorkConcurrentUpdateTest() throws InterruptedException {
        int numberOfThreads = 10_000;

        final int id = productCategoryRepository.create(
                new ProductCategory(
                        0,
                        "Category"
                )
        );

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
            for(int i = 0; i < numberOfThreads; i++ ) {
                final int finalI = i;

                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        productCategoryRepository.update(
                                id,
                                new ProductCategory(
                                        0,
                                        "Category" + finalI
                                )
                        );
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

            ProductCategory category = productCategoryRepository.get(id);

            assertNotNull(category);
            assertTrue(category.name().startsWith("Category"));
        }
    }

    @Test
    public void shouldWorkConcurrentMixTest() throws InterruptedException {
        int numberOfThreads = 10_000;

        try(ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads)) {
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
            for(int i = 0; i < numberOfThreads; i++ ) {
                final int finalI = i;

                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        int id = productCategoryRepository.create(
                                new ProductCategory(
                                        0,
                                        "Category" + finalI
                                )
                        );
                        productCategoryRepository.update(id, new ProductCategory(0,"Category" + finalI + "_2"));
                        productCategoryRepository.delete(id);
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

            assertEquals(0, productCategoryRepository.searchCategories(new SearchCategoriesRequest(null, null, null)).items().size());
        }
    }
}
