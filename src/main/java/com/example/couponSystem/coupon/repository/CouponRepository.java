package com.example.couponSystem.coupon.repository;

import com.example.couponSystem.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
