package rw.ac.auca.scoutpro_27202.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rw.ac.auca.scoutpro_27202.domain.AuthProvider;
import rw.ac.auca.scoutpro_27202.domain.Role;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.repository.RoleRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    // runs once, every time the app starts
    @Override
    public void run(String... args) {

        // 1. create the four roles if they don't exist yet
        for (String name : List.of("ADMIN", "SCOUT", "CLUB_MANAGER", "ATHLETE")) {
            if (roleRepo.findByName(name).isEmpty()) {
                roleRepo.save(new Role(name));
            }
        }

        // 2. create the first admin if missing
        if (!userRepo.existsByEmail(adminEmail)) {
            User admin = new User(adminEmail, passwordEncoder.encode(adminPassword), AuthProvider.LOCAL);
            admin.getRoles().add(roleRepo.findByName("ADMIN").orElseThrow());
            userRepo.save(admin);
        }
    }
}