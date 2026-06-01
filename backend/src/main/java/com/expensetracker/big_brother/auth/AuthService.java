package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.auth.dto.AuthResponse;
import com.expensetracker.big_brother.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
class AuthService {
    private JwtService jwtService;
    public AuthService(JwtService jwtService) {this.jwtService = jwtService;}

    // Register
    public AuthResponse register(@Valid RegisterRequest request) {
        
    }

    // Login
}

