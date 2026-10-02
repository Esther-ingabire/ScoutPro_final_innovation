package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);                 // login
    Optional<User> findByProviderId(String providerId);       // Google login
    boolean existsByEmail(String email);
}