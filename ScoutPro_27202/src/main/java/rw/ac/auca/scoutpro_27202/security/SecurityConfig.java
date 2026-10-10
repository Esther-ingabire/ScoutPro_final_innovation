package rw.ac.auca.scoutpro_27202.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           OAuth2LoginSuccessHandler oAuth2SuccessHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // a session exists ONLY during the Google handshake; the API itself uses JWTs
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()            // password login/register
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll() // Google login flow
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                // Google login: on success, our handler issues the JWT
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((request, response, ex) ->
                                response.sendRedirect(frontendUrl + "/login?error=oauth_failed"))
                )
                // every API request: validate our JWT
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // an API call without a token gets 401, not a redirect to Google's login page
                .exceptionHandling(e -> e.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        rolesConverter.setAuthoritiesClaimName("roles");
        rolesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
        return converter;
    }
}