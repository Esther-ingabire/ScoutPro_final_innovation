package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Scout;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.repository.AssessmentRepository;
import rw.ac.auca.scoutpro_27202.repository.ScoutRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;
import rw.ac.auca.scoutpro_27202.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@Service
public class ScoutService {

    @Autowired
    private ScoutRepository scoutRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AssessmentRepository assessmentRepo;

    @Autowired
    private CurrentUser currentUser;

    // CREATE a scout profile for an existing user who has the SCOUT role
    @PreAuthorize("hasRole('ADMIN')")
    public Scout saveScout(UUID userId, Scout scout) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isScout = user.getRoles().stream().anyMatch(r -> r.getName().equals("SCOUT"));
        if (!isScout) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "User must have the SCOUT role before a scout profile is created");
        }
        if (scoutRepo.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user already has a scout profile");
        }

        validate(scout);
        String code = scout.getScoutCode().trim().toUpperCase();
        if (scoutRepo.findByScoutCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Scout code already exists");
        }

        scout.setScoutCode(code);
        scout.setEmail(user.getEmail());   // email always comes from the account, never typed twice
        scout.setUser(user);
        scout.setActive(true);
        return scoutRepo.save(scout);
    }

    // READ all
    @PreAuthorize("hasRole('ADMIN')")
    public List<Scout> getAllScouts() {
        return scoutRepo.findAll();
    }

    // READ one
    @PreAuthorize("hasRole('ADMIN')")
    public Scout getScoutById(UUID id) {
        return scoutRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scout not found"));
    }

    // READ my own profile: used by scouts, and later by AssessmentService
    @PreAuthorize("hasRole('SCOUT')")
    public Scout getMyScoutProfile() {
        return scoutRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You don't have a scout profile yet; ask an admin to create one"));
    }

    // UPDATE: an admin, or the scout themselves
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    public Scout updateScout(UUID id, Scout newData) {
        Scout existing = scoutRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scout not found"));

        // ownership: a scout may only edit their own profile
        boolean isOwner = existing.getUser().getId().equals(currentUser.getId());
        if (!currentUser.hasRole("ADMIN") && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own profile");
        }

        if (newData.getFullName() == null || newData.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        existing.setFullName(newData.getFullName());
        existing.setPhoneNumber(newData.getPhoneNumber());
        existing.setOrganization(newData.getOrganization());
        return scoutRepo.save(existing);
    }

    // DEACTIVATE
    @PreAuthorize("hasRole('ADMIN')")
    public Scout deactivateScout(UUID id) {
        Scout scout = getScoutById(id);
        scout.setActive(false);
        return scoutRepo.save(scout);
    }

    // DELETE (only scouts with no assessments)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteScout(UUID id) {
        if (!scoutRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scout not found");
        }
        if (assessmentRepo.existsByScoutId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Scout has recorded assessments; deactivate instead");
        }
        scoutRepo.deleteById(id);
    }

    private void validate(Scout scout) {
        if (scout.getScoutCode() == null || scout.getScoutCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scout code is required");
        }
        if (scout.getFullName() == null || scout.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }
    }
}