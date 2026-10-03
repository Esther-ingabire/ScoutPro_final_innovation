package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.dto.AuthResponse;
import rw.ac.auca.scoutpro_27202.dto.LoginRequest;
import rw.ac.auca.scoutpro_27202.dto.RegisterRequest;
import rw.ac.auca.scoutpro_27202.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
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