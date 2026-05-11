package com.example.couponSystem;

import com.example.couponSystem.coupon.repository.CouponIssueRepository;
import com.example.couponSystem.coupon.service.CouponIssueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class CouponIssueServiceTest {

    @Autowired
    private CouponIssueService couponIssueService;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Test
    void 동시에_1000명이_쿠폰을_발급받는다() throws InterruptedException {
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    couponIssueService.issueCoupon(4L, userId);
                } catch (Exception e) {
                    // 재고 소진 예외는 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long count = couponIssueRepository.countByCouponId(4L);
        System.out.println("발급된 쿠폰 수: " + count);
        assertThat(count).isEqualTo(10);
    }
}
