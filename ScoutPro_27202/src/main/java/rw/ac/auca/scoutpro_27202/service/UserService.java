package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Role;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.dto.PageRequests;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.messaging.AuditRecorder;
import rw.ac.auca.scoutpro_27202.messaging.Snapshots;
import rw.ac.auca.scoutpro_27202.repository.RoleRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private AuditRecorder auditRecorder;

    // READ all
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<User> getAllUsers(int page, int size) {
        return PageResponse.of(userRepo.findAll(PageRequests.of(page, size)));
    }

    // READ one
    @PreAuthorize("hasRole('ADMIN')")
    public User getUserById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // REPLACE a user's roles
    @PreAuthorize("hasRole('ADMIN')")
    public User updateRoles(UUID id, List<String> roleNames, UUID currentAdminId) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
        }

        User user = getUserById(id);
        String before = Snapshots.of("roles", roleNames(user));

        // turn ["scout"] into real Role rows; unknown names are rejected
        Set<Role> newRoles = new HashSet<>();
        for (String name : roleNames) {
            Role role = roleRepo.findByName(name.trim().toUpperCase())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Unknown role: " + name));
            newRoles.add(role);
        }

        // an admin can't remove their own ADMIN role (could lock everyone out)
        boolean removingOwnAdmin = id.equals(currentAdminId)
                && newRoles.stream().noneMatch(r -> r.getName().equals("ADMIN"));
        if (removingOwnAdmin) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "You cannot remove your own ADMIN role");
        }

        user.getRoles().clear();
        user.getRoles().addAll(newRoles);
        User saved = userRepo.save(user);
        auditRecorder.changed("user", saved.getId().toString(), "roles-updated", before,
                Snapshots.of("roles", roleNames(saved)));
        return saved;
    }

    private String roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.joining(","));
    }
}