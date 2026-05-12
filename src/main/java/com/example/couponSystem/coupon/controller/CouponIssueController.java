package com.example.couponSystem.coupon.controller;

import com.example.couponSystem.coupon.dto.CouponIssueRequest;
import com.example.couponSystem.coupon.service.CouponIssueRequestService;
import com.example.couponSystem.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupon-issues")
@RequiredArgsConstructor
public class CouponIssueController {

    private final CouponIssueRequestService couponIssueRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse> issueCoupon(@RequestBody CouponIssueRequest request) {
        couponIssueRequestService.issueCoupon(request.couponId(), request.userId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
