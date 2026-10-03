package rw.ac.auca.scoutpro_27202.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import rw.ac.auca.scoutpro_27202.domain.AuthProvider;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.messaging.EventPublisher;
import rw.ac.auca.scoutpro_27202.repository.RoleRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// runs after Google confirms who the user is: find/create the user, issue OUR JWT
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EventPublisher eventPublisher;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String googleId = googleUser.getAttribute("sub");          // Google's permanent user id
        String email = googleUser.getAttribute("email");
        Boolean emailVerified = googleUser.getAttribute("email_verified");

        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            endLoginSession(request);
            response.sendRedirect(frontendUrl + "/login?error=email_not_verified");
            return;
        }
        email = email.toLowerCase();

        // 1. returning Google user, 2. existing account with the same email, 3. brand-new user
        User user = userRepo.findByProviderId(googleId)
                .or(() -> userRepo.findByEmail(googleUser.<String>getAttribute("email").toLowerCase()))
                .orElse(null);

        if (user == null) {
            // US3: a new Google user gets an account with the ATHLETE role
            user = new User(email, null, AuthProvider.GOOGLE);   // no password: Google verifies them
            user.setProviderId(googleId);
            user.getRoles().add(roleRepo.findByName("ATHLETE").orElseThrow());
            userRepo.save(user);

            Map<String, String> data = new HashMap<>();
            data.put("userId", user.getId().toString());
            data.put("email", user.getEmail());
            eventPublisher.publish("user.registered", data);     // welcome email, as for password signup

        } else if (user.getProviderId() == null) {
            // existing password account with the same VERIFIED email: link it to Google
            user.setProviderId(googleId);
            userRepo.save(user);
        }

        if (!user.isEnabled()) {
            endLoginSession(request);
            response.sendRedirect(frontendUrl + "/login?error=account_disabled");
            return;
        }

        String token = jwtService.generateToken(user);
        endLoginSession(request);

        // the token goes in the #fragment: browsers never send fragments to servers or logs
        response.sendRedirect(frontendUrl + "/oauth2/callback#token=" + token);
    }

    // the session was only needed during the Google handshake; from now on the JWT is used
    private void endLoginSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}