package rw.ac.auca.scoutpro_27202.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    // null for users who sign in with Google
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    // Google's id for this user, null for LOCAL users
    @Column(unique = true)
    private String providerId;

    private boolean enabled;

    // null means this account was created before email codes existed, so it may sign in.
    // false means a new password signup must enter the emailed code first.
    private Boolean emailVerified;

    @JsonIgnore
    private String otpHash;

    @JsonIgnore
    private Instant otpExpiresAt;

    @JsonIgnore
    private Integer otpAttempts;

    // join table user_roles(user_id, role_id)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User() {}

    public User(String email, String password, AuthProvider provider) {
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.enabled = true;
    }

    // generate getters and setters

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public Instant getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(Instant otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public Integer getOtpAttempts() {
        return otpAttempts;
    }

    public void setOtpAttempts(Integer otpAttempts) {
        this.otpAttempts = otpAttempts;
    }
}