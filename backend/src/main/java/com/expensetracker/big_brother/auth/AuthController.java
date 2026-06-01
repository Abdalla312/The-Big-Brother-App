package com.expensetracker.big_brother.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

//    POST /api/v1/auth/register
//    POST /api/v1/auth/login
//    GET  /api/v1/auth/verify-email

}
