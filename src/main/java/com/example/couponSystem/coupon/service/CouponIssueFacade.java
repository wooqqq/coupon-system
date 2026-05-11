package com.example.couponSystem.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueFacade {

    private final CouponIssueService couponIssueService;

    public void issueCoupon(Long couponId, Long userId) {
        int maxRetry = 10;
        for  (int i = 0; i < maxRetry; i++) {
            try {
                couponIssueService.issueCoupon(couponId, userId);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                // 충돌 감지 → 재시도
            }
        }
        throw new RuntimeException("쿠폰 발급 실패 (재시도 횟수 초과)");
    }
}
