package rw.ac.auca.scoutpro_27202.dto;

import java.util.List;

// what the client receives after login or register
public record AuthResponse(String token, String email, List<String> roles, long expiresInMinutes) {}