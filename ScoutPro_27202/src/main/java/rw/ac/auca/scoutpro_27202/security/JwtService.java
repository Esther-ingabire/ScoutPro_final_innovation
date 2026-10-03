package rw.ac.auca.scoutpro_27202.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import rw.ac.auca.scoutpro_27202.domain.Role;
import rw.ac.auca.scoutpro_27202.domain.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtService {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generateToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("scoutpro")
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(user.getEmail())                    // who this token belongs to
                .claim("userId", user.getId().toString())    // used later: "which scout is this?"
                .claim("roles", roles)                       // e.g. ["SCOUT"]
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}