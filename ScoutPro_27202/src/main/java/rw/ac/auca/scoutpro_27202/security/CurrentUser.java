package rw.ac.auca.scoutpro_27202.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

// answers "who is making this request?" from the verified JWT
@Component
public class CurrentUser {

    // the logged-in user's id, read from the token's "userId" claim
    public UUID getId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }

    // true if the token contains this role, e.g. hasRole("ADMIN")
    public boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}