package com.example.couponSystem;

import com.example.couponSystem.coupon.entity.Coupon;
import com.example.couponSystem.coupon.repository.CouponIssueRepository;
import com.example.couponSystem.coupon.repository.CouponRepository;
import com.example.couponSystem.coupon.service.CouponIssueFacade;
import com.example.couponSystem.coupon.service.CouponIssueService;
import org.junit.jupiter.api.BeforeEach;
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
    private CouponIssueFacade couponIssueFacade;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Long couponId;

    @BeforeEach
    void setUp() {
        Coupon coupon = new Coupon();
        coupon.setName("낙관적락 테스트 쿠폰");
        coupon.setTotalQuantity(10);
        coupon.setIssuedQuantity(0);
        couponId = couponRepository.save(coupon).getId();
    }

    @Test
    void 동시에_1000명이_쿠폰을_발급받는다() throws InterruptedException {
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    couponIssueFacade.issueCoupon(couponId, userId);
                } catch (Exception e) {
                    // 재고 소진 예외는 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        long start = System.currentTimeMillis();

        latch.await();

        long end = System.currentTimeMillis();
        System.out.println("실행 시간: " + (end - start) + "ms");

        long count = couponIssueRepository.countByCouponId(couponId);
        System.out.println("발급된 쿠폰 수: " + count);
        assertThat(count).isEqualTo(10);
    }
}
