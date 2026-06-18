package com.expensetracker.big_brother.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String name;
    private String email;
    private boolean verified;
    private String message;
}
