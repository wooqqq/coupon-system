package com.example.couponSystem.coupon.service;

import com.example.couponSystem.coupon.entity.Coupon;
import com.example.couponSystem.coupon.entity.CouponIssue;
import com.example.couponSystem.coupon.repository.CouponIssueRepository;
import com.example.couponSystem.coupon.repository.CouponRepository;
import com.example.couponSystem.user.entity.User;
import com.example.couponSystem.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final CouponIssueRepository couponIssueRepository;

    public Long issueCoupon(Long couponId, Long userId) {
        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 3. 중복 발급 확인
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(couponId)
                .userId(userId)
                .build();

        return couponIssueRepository.save(couponIssue).getId();

    }
}
