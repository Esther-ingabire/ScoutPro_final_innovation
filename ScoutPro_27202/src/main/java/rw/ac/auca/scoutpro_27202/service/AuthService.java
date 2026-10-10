package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.AuthProvider;
import rw.ac.auca.scoutpro_27202.domain.Role;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.dto.AuthResponse;
import rw.ac.auca.scoutpro_27202.dto.LoginRequest;
import rw.ac.auca.scoutpro_27202.dto.OtpRequest;
import rw.ac.auca.scoutpro_27202.dto.RegisterRequest;
import rw.ac.auca.scoutpro_27202.dto.RegisterResponse;
import rw.ac.auca.scoutpro_27202.dto.ResendOtpRequest;
import rw.ac.auca.scoutpro_27202.messaging.AuditRecorder;
import rw.ac.auca.scoutpro_27202.messaging.EventPublisher;
import rw.ac.auca.scoutpro_27202.messaging.Snapshots;
import rw.ac.auca.scoutpro_27202.repository.RoleRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;
import rw.ac.auca.scoutpro_27202.security.JwtService;
import rw.ac.auca.scoutpro_27202.security.RefreshTokenService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Service
public class AuthService {

    static final String NEEDS_CODE = "Enter the 6-digit code we emailed you before signing in.";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_MINUTES = 10;
    private static final int OTP_MAX_ATTEMPTS = 5;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EventPublisher eventPublisher;   // EVENT

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AuditRecorder auditRecorder;

    @Value("${app.admin.email}")
    private String adminEmail;

    // REGISTER: every self-registered user starts as ATHLETE; an admin can grant other roles.
    // No token yet. The user must enter the emailed code on the confirmation page.
    public RegisterResponse register(RegisterRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), AuthProvider.LOCAL);
        Role athleteRole = roleRepo.findByName("ATHLETE").orElseThrow();
        user.getRoles().add(athleteRole);
        user.setEmailVerified(false);
        String code = issueCode(user);
        userRepo.save(user);

        // EVENT: welcome email carries the code. The admin alert does not.
        Map<String, String> data = new HashMap<>();
        data.put("userId", user.getId().toString());
        data.put("email", user.getEmail());
        data.put("adminEmail", adminEmail);   // second email: ask an admin to assign a role
        data.put("otp", code);
        eventPublisher.publish("user.registered", data);
        auditRecorder.changedAs(user.getId().toString(), "user", user.getId().toString(),
                "created", null, Snapshots.of("email", user.getEmail(), "provider", "LOCAL"));

        return new RegisterResponse(email, "We emailed you a 6-digit code. Enter it to finish signing up.");
    }

    // The code is checked here. A match is what signs the new user in.
    public AuthResponse verifyOtp(OtpRequest request) {
        User user = awaitingCode(request.email());
        String code = request.code() == null ? "" : request.code().trim();
        if (!code.matches("\\d{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the 6-digit code from the email.");
        }
        if (user.getOtpHash() == null || user.getOtpExpiresAt() == null
                || user.getOtpExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That code has expired. Ask for a new one.");
        }
        int attempts = user.getOtpAttempts() == null ? 0 : user.getOtpAttempts();
        if (attempts >= OTP_MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Too many wrong codes. Ask for a new one.");
        }
        if (!MessageDigest.isEqual(hash(code).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                user.getOtpHash().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            user.setOtpAttempts(attempts + 1);
            userRepo.save(user);
            int left = OTP_MAX_ATTEMPTS - (attempts + 1);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    left == 0 ? "That code is wrong. Ask for a new one." : "That code is wrong. " + left + " tries left.");
        }

        user.setEmailVerified(true);
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        userRepo.save(user);
        return buildResponse(user);
    }

    public RegisterResponse resendOtp(ResendOtpRequest request) {
        User user = awaitingCode(request.email());
        String code = issueCode(user);
        userRepo.save(user);

        Map<String, String> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("otp", code);
        eventPublisher.publish("user.otp.sent", data);
        return new RegisterResponse(user.getEmail(), "A new code is on its way. It expires in 10 minutes.");
    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {
        if (request.email() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        User user = userRepo.findByEmail(request.email().trim().toLowerCase()).orElse(null);

        // same message whether the email or the password is wrong
        if (user == null
                || user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NEEDS_CODE);
        }

        return buildResponse(user);
    }

    // Exchange a refresh token for a new access token and a new refresh token.
    // The old refresh token is revoked, so a stolen copy cannot be reused.
    public AuthResponse refresh(String refreshToken) {
        User user = refreshTokenService.rotate(refreshToken);
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NEEDS_CODE);
        }
        return buildResponse(user);
    }

    private User awaitingCode(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        User user = userRepo.findByEmail(rawEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account is waiting for a code."));
        if (!Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This account is already confirmed. Sign in.");
        }
        return user;
    }

    // Stores only the hash. The plain code is returned so it can go into the email.
    private String issueCode(User user) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setOtpHash(hash(code));
        user.setOtpExpiresAt(Instant.now().plus(OTP_MINUTES, ChronoUnit.MINUTES));
        user.setOtpAttempts(0);
        return code;
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                refreshTokenService.issue(user),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).toList(),
                jwtService.getExpirationMinutes());
    }
}