package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.AuthProvider;
import rw.ac.auca.scoutpro_27202.domain.Role;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.dto.AuthResponse;
import rw.ac.auca.scoutpro_27202.dto.LoginRequest;
import rw.ac.auca.scoutpro_27202.dto.RegisterRequest;
import rw.ac.auca.scoutpro_27202.repository.RoleRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;
import rw.ac.auca.scoutpro_27202.security.JwtService;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // REGISTER: every self-registered user starts as ATHLETE; an admin can grant other roles
    public AuthResponse register(RegisterRequest request) {
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
        userRepo.save(user);

        return buildResponse(user);
    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {
        if (request.email() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        User user = userRepo.findByEmail(request.email().trim().toLowerCase()).orElse(null);

        // same message whether the email or the password is wrong (see note below)
        if (user == null
                || user.getPassword() == null                          // Google-only account
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).toList(),
                jwtService.getExpirationMinutes());
    }
}