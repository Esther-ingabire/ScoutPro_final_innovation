package rw.ac.auca.scoutpro_27202.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.RefreshToken;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.repository.RefreshTokenRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private RefreshTokenRepository refreshTokenRepo;

    @Value("${jwt.refresh-days}")
    private long refreshDays;

    // Returns the raw token (give this to the browser). The database stores only the hash.
    // SHA-256 is used instead of BCrypt because we have to FIND the row by the hash.
    // BCrypt produces a different hash every time, so it cannot be looked up.
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken row = new RefreshToken();
        row.setUser(user);
        row.setTokenHash(hash(raw));
        row.setExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        row.setRevoked(false);
        refreshTokenRepo.save(row);
        return raw;
    }

    // Checks the token, marks it used (revoked), and returns the user.
    // The caller then issues a new access token and a new refresh token.
    @Transactional
    public User rotate(String raw) {
        RefreshToken row = findUsable(raw);
        row.setRevoked(true);
        User user = row.getUser();
        user.getRoles().size(); // load roles while the session is open
        return user;
    }

    @Transactional
    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        refreshTokenRepo.findByTokenHash(hash(raw)).ifPresent(row -> row.setRevoked(true));
    }

    private RefreshToken findUsable(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
        RefreshToken row = refreshTokenRepo.findByTokenHash(hash(raw))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));
        if (row.isRevoked() || row.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
        return row;
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }
}
