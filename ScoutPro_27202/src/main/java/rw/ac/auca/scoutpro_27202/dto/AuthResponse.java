package rw.ac.auca.scoutpro_27202.dto;

import java.util.List;

// token is the short-lived access JWT. refreshToken is the 7-day token stored only as a hash.
public record AuthResponse(
        String token,
        String refreshToken,
        String email,
        List<String> roles,
        long expiresInMinutes
) {}
