package com.example.couponSystem.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CouponIssueFacade {

    private final CouponIssueService couponIssueService;

    private static final int MAX_RETRY = 5;
    private static final long BASE_DELAY_MS = 50;
    private static final long MAX_DELAY_MS = 500;

    public void issueCoupon(Long couponId, Long userId) {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                couponIssueService.issueCoupon(couponId, userId);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (i == MAX_RETRY - 1) {
                    throw new RuntimeException("쿠폰 발급 실패 (재시도 횟수 초과)");
                }
                long delay = Math.min(BASE_DELAY_MS * (1L << i), MAX_DELAY_MS);
                long jitter = ThreadLocalRandom.current().nextLong(delay / 2);
                try {
                    Thread.sleep(delay + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}