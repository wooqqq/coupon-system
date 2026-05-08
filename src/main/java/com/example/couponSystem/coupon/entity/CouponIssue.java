package com.example.couponSystem.coupon.entity;

import com.example.couponSystem.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "coupon_issues")
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime regDt;

    @Builder
    public CouponIssue(Coupon coupon, User user) {
        this.coupon = coupon;
        this.user = user;
        this.regDt = LocalDateTime.now();
    }

}
