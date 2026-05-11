package com.example.couponSystem.coupon.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CouponIssueRequestService {

    private final RedissonClient redissonClient;
    private final CouponIssueService couponIssueService;

    public void issueCoupon(Long couponId, Long userId) {
        RLock lock = redissonClient.getLock("coupon:lock:" + couponId);
        try {
            boolean acquired = lock.tryLock(10, 3, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("락 획득 실패");
            }
            couponIssueService.issueCoupon(couponId, userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
