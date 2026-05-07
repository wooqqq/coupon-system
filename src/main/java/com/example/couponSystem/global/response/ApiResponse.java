package com.example.couponSystem.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private String resultCode;
    private String resultMessage;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("Success", "성공", data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>("Fail", message, null);
    }
}
