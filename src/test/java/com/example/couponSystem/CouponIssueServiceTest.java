package com.example.couponSystem;

import com.example.couponSystem.coupon.entity.Coupon;
import com.example.couponSystem.coupon.repository.CouponIssueRepository;
import com.example.couponSystem.coupon.repository.CouponRepository;
import com.example.couponSystem.coupon.service.CouponIssueRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

// 사전조건: users 테이블에 userId 1~1000 데이터가 존재해야 합니다.
@SpringBootTest
public class CouponIssueServiceTest {

    @Autowired
    private CouponIssueRequestService couponIssueRequestService;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Long couponId;

    @AfterEach
    void tearDown() {
        couponIssueRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    void 동시에_1000명이_쿠폰을_발급받는다() throws InterruptedException {
        int repeat = 5;
        long totalTime = 0;

        for (int r = 0; r < repeat; r++) {
            couponIssueRepository.deleteAll();
            couponRepository.deleteAll();

            Coupon coupon = new Coupon();
            coupon.setName("분산락 테스트 쿠폰");
            coupon.setTotalQuantity(10);
            coupon.setIssuedQuantity(0);
            couponId = couponRepository.save(coupon).getId();

            ExecutorService executorService = Executors.newFixedThreadPool(32);
            CountDownLatch latch = new CountDownLatch(1000);

            long start = System.currentTimeMillis();

            for (int i = 1; i <= 1000; i++) {
                long userId = i;
                executorService.submit(() -> {
                    try {
                        couponIssueRequestService.issueCoupon(couponId, userId);
                    } catch (Exception e) {
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            totalTime += System.currentTimeMillis() - start;
            executorService.shutdown();

            long count = couponIssueRepository.countByCouponId(couponId);
            System.out.println("[" + (r + 1) + "회] 발급된 쿠폰 수: " + count + " / 실행 시간: " + (System.currentTimeMillis() - start) + "ms");
            assertThat(count).isEqualTo(10);
        }

        System.out.println("평균 실행 시간: " + (totalTime / repeat) + "ms");
    }
}
