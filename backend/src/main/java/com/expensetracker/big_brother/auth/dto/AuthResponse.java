package com.expensetracker.big_brother.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String name,
        String email,
        boolean verified
) {

}
