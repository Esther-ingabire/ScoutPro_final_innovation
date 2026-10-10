package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Assessment;
import rw.ac.auca.scoutpro_27202.domain.Athlete;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.domain.Team;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.dto.PageRequests;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.messaging.AuditRecorder;
import rw.ac.auca.scoutpro_27202.messaging.EventPublisher;
import rw.ac.auca.scoutpro_27202.messaging.Snapshots;
import rw.ac.auca.scoutpro_27202.repository.AssessmentRepository;
import rw.ac.auca.scoutpro_27202.repository.AthleteRepository;
import rw.ac.auca.scoutpro_27202.repository.PhysicalProfileRepository;
import rw.ac.auca.scoutpro_27202.repository.ShortlistRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;
import rw.ac.auca.scoutpro_27202.security.CurrentUser;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AthleteService {

    @Autowired
    private AthleteRepository athleteRepo;

    @Autowired
    private AssessmentRepository assessmentRepo;

    @Autowired
    private ShortlistRepository shortlistRepo;

    @Autowired
    private PhysicalProfileRepository profileRepo;

    @Autowired
    private SportService sportService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private EventPublisher eventPublisher;   // EVENT

    @Autowired
    private AuditRecorder auditRecorder;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private UserRepository userRepo;

    // CREATE
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    public Athlete saveAthlete(UUID sportId, UUID teamId, Athlete athlete) {
        Sport sport = sportService.getSportById(sportId);
        validate(athlete);

        String code = athlete.getAthleteCode().trim().toUpperCase();
        if (athleteRepo.existsByAthleteCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Athlete code already exists");
        }

        athlete.setAthleteCode(code);
        athlete.setSport(sport);
        athlete.setTeam(resolveTeam(teamId, sportId));
        athlete.setActive(true);
        Athlete saved = athleteRepo.save(athlete);
        auditRecorder.changed("athlete", saved.getId().toString(), "created", null, athleteSnapshot(saved));
        return saved;
    }

    // READ all — staff only. An athlete uses getMyAthlete instead.
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    public PageResponse<Athlete> getAllAthletes(int page, int size) {
        return PageResponse.of(athleteRepo.findAll(PageRequests.of(page, size)));
    }

    // READ one. Staff may open any athlete. An ATHLETE may open only the row linked to their login.
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER', 'ATHLETE')")
    public Athlete getAthleteById(UUID id) {
        Athlete athlete = athleteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));
        checkAthleteOwnership(athlete);
        return athlete;
    }

    @PreAuthorize("hasRole('ATHLETE')")
    public Athlete getMyAthlete() {
        return athleteRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No athlete profile is linked to your account"));
    }

    @PreAuthorize("hasRole('ATHLETE')")
    public Athlete updateMyContact(String contactNumber) {
        if (contactNumber == null || contactNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contact number is required");
        }
        Athlete athlete = getMyAthlete();
        String before = athleteSnapshot(athlete);
        athlete.setContactNumber(contactNumber.trim());
        Athlete saved = athleteRepo.save(athlete);
        auditRecorder.changed("athlete", saved.getId().toString(), "contact-updated", before, athleteSnapshot(saved));
        return saved;
    }

    // Admin links (or unlinks) a login account. userId null removes the link.
    @PreAuthorize("hasRole('ADMIN')")
    public Athlete linkAccount(UUID id, UUID userId) {
        Athlete athlete = athleteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));
        String before = athleteSnapshot(athlete);
        if (userId == null) {
            athlete.setUser(null);
        } else {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            athleteRepo.findByUserId(userId).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "That account is already linked to another athlete");
                }
            });
            athlete.setUser(user);
        }
        Athlete saved = athleteRepo.save(athlete);
        auditRecorder.changed("athlete", saved.getId().toString(), "account-linked", before, athleteSnapshot(saved));
        return saved;
    }

    // UPDATE
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    public Athlete updateAthlete(UUID id, UUID teamId, Athlete newData) {
        Athlete existing = getAthleteById(id);
        validate(newData);

        String code = newData.getAthleteCode().trim().toUpperCase();
        Athlete sameCode = athleteRepo.findByAthleteCode(code).orElse(null);
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Athlete code already exists");
        }

        String before = athleteSnapshot(existing);
        existing.setAthleteCode(code);
        existing.setFullName(newData.getFullName());
        existing.setDateOfBirth(newData.getDateOfBirth());
        existing.setPosition(newData.getPosition());
        existing.setNationality(newData.getNationality());
        existing.setContactNumber(newData.getContactNumber());
        existing.setTeam(resolveTeam(teamId, existing.getSport().getId()));
        Athlete saved = athleteRepo.save(existing);
        auditRecorder.changed("athlete", saved.getId().toString(), "updated", before, athleteSnapshot(saved));
        return saved;
    }

    // DEACTIVATE
    @PreAuthorize("hasRole('ADMIN')")
    public Athlete deactivateAthlete(UUID id) {
        Athlete athlete = getAthleteById(id);
        athlete.setActive(false);
        Athlete saved = athleteRepo.save(athlete);

        // EVENT: inform scouts and write to audit log
        Map<String, String> data = new HashMap<>();
        data.put("athleteId", saved.getId().toString());
        data.put("athleteName", saved.getFullName());
        data.put("scoutEmails", scoutEmails(saved.getId()));
        eventPublisher.publish("athlete.deactivated", data);
        auditRecorder.changed("athlete", saved.getId().toString(), "deactivated", null,
                Snapshots.of("active", false, "fullName", saved.getFullName()));

        return saved;
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteAthlete(UUID id) {
        if (!athleteRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found");
        }
        if (assessmentRepo.existsByAthleteId(id) || shortlistRepo.existsByAthletes_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Athlete has assessments or is shortlisted; deactivate instead");
        }
        profileRepo.findByAthleteId(id).ifPresent(profileRepo::delete);
        athleteRepo.deleteById(id);
        auditRecorder.changed("athlete", id.toString(), "deleted", null, null);
    }

    // ---------- helpers ----------

    private Team resolveTeam(UUID teamId, UUID sportId) {
        if (teamId == null) {
            return null;
        }
        Team team = teamService.getTeamById(teamId);
        if (!team.getSport().getId().equals(sportId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Team plays a different sport than the athlete");
        }
        return team;
    }

    private void checkAthleteOwnership(Athlete athlete) {
        if (isStaff()) {
            return;
        }
        boolean own = athlete.getUser() != null && athlete.getUser().getId().equals(currentUser.getId());
        if (!own) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own athlete profile");
        }
    }

    private boolean isStaff() {
        return currentUser.hasRole("ADMIN") || currentUser.hasRole("SCOUT") || currentUser.hasRole("CLUB_MANAGER");
    }

    private String scoutEmails(UUID athleteId) {
        Set<String> emails = new LinkedHashSet<>();
        for (Assessment assessment : assessmentRepo.findWithScoutByAthleteId(athleteId)) {
            String email = assessment.getScout().getEmail();
            if (email != null && !email.isBlank()) {
                emails.add(email);
            }
        }
        return String.join(",", emails);
    }

    private String athleteSnapshot(Athlete athlete) {
        return Snapshots.of(
                "code", athlete.getAthleteCode(),
                "fullName", athlete.getFullName(),
                "position", athlete.getPosition(),
                "contact", athlete.getContactNumber(),
                "active", athlete.isActive(),
                "userId", athlete.getUser() == null ? null : athlete.getUser().getId());
    }

    private void validate(Athlete athlete) {
        if (athlete.getAthleteCode() == null || athlete.getAthleteCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Athlete code is required");
        }
        if (athlete.getFullName() == null || athlete.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }
        if (athlete.getDateOfBirth() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth is required");
        }
        if (!athlete.getDateOfBirth().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth must be in the past");
        }
    }
}