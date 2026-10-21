package com.labresource.platform.controller;


import com.labresource.platform.dto.LoginRequest;
import com.labresource.platform.dto.LoginResponse;
import com.labresource.platform.dto.RegisterRequest;
import com.labresource.platform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request)
    {
        String message=authService.register(request);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest request)
    {
        LoginResponse  response=authService.login(request);
        return ResponseEntity.ok(response);
    }
}
