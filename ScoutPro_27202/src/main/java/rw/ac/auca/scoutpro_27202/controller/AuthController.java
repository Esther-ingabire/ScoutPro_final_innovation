package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.dto.AuthResponse;
import rw.ac.auca.scoutpro_27202.dto.LoginRequest;
import rw.ac.auca.scoutpro_27202.dto.OtpRequest;
import rw.ac.auca.scoutpro_27202.dto.RefreshRequest;
import rw.ac.auca.scoutpro_27202.dto.RegisterRequest;
import rw.ac.auca.scoutpro_27202.dto.RegisterResponse;
import rw.ac.auca.scoutpro_27202.dto.ResendOtpRequest;
import rw.ac.auca.scoutpro_27202.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/verify-otp")
    public AuthResponse verifyOtp(@RequestBody OtpRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/auth/resend-otp")
    public RegisterResponse resendOtp(@RequestBody ResendOtpRequest request) {
        return authService.resendOtp(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    // needs a token: shows what the server reads from it
    @GetMapping("/users/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "email", jwt.getSubject(),
                "userId", jwt.getClaimAsString("userId"),
                "roles", jwt.getClaimAsStringList("roles"),
                "expiresAt", jwt.getExpiresAt()
        );
    }
}