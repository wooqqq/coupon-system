package com.example.couponSystem.coupon.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int totalQuantity;

    private int issuedQuantity;

    public void issue() {
        if (this.issuedQuantity >= this.totalQuantity) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }
        this.issuedQuantity++;
    }

}