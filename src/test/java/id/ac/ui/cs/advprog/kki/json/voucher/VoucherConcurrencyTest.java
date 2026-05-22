package id.ac.ui.cs.advprog.kki.json.voucher;

import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import id.ac.ui.cs.advprog.kki.json.voucher.repository.VoucherRepository;
import id.ac.ui.cs.advprog.kki.json.voucher.service.VoucherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves that pessimistic locking (findByIdForUpdate) prevents the quota from
 * going below zero when many threads race to use the same voucher simultaneously.
 *
 * Scenario: 5 available uses, 20 concurrent threads → exactly 5 succeed, 15 fail,
 * and the final quota is 0 (never negative).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:conctest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VoucherConcurrencyTest {

    private static final String TEST_CODE = "CONCTEST";
    private static final int QUOTA = 5;
    private static final int THREADS = 20;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @AfterEach
    void cleanup() {
        voucherRepository.deleteById(TEST_CODE);
    }

    @Test
    void useVoucher_underConcurrentLoad_quotaNeverGoesNegative() throws InterruptedException {
        Instant now = Instant.now();
        voucherRepository.save(new Voucher(
                TEST_CODE, QUOTA,
                now.minus(1, ChronoUnit.DAYS),
                now.plus(7, ChronoUnit.DAYS),
                "concurrency test", DiscountType.FLAT, 1000.0, true
        ));

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            final int idx = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    voucherService.useVoucher(TEST_CODE, "order-" + idx, "user-" + idx);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown(); // fire all threads at once
        boolean completed = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should finish within 30 seconds");

        Voucher result = voucherRepository.findById(TEST_CODE).orElseThrow();

        assertEquals(QUOTA, successCount.get(),
                "Exactly QUOTA threads should succeed");
        assertEquals(THREADS - QUOTA, failCount.get(),
                "Remaining threads should be rejected");
        assertEquals(0, result.getQuota(),
                "Final quota must be exactly 0");
        assertTrue(result.getQuota() >= 0,
                "Quota must never go negative — pessimistic lock failed!");
    }
}
