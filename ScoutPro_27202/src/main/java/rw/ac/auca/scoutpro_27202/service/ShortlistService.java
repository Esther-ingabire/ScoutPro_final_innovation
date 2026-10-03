package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Athlete;
import rw.ac.auca.scoutpro_27202.domain.Shortlist;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.dto.ShortlistAthleteResponse;
import rw.ac.auca.scoutpro_27202.dto.ShortlistRequest;
import rw.ac.auca.scoutpro_27202.dto.ShortlistResponse;
import rw.ac.auca.scoutpro_27202.messaging.EventPublisher;
import rw.ac.auca.scoutpro_27202.repository.ShortlistRepository;
import rw.ac.auca.scoutpro_27202.repository.UserRepository;
import rw.ac.auca.scoutpro_27202.security.CurrentUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShortlistService {

    @Autowired
    private ShortlistRepository shortlistRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AthleteService athleteService;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private EventPublisher eventPublisher;   // EVENT

    // CREATE: the owner is whoever is logged in
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_MANAGER')")
    @Transactional
    public ShortlistResponse createShortlist(ShortlistRequest request) {
        String name = validateName(request);
        UUID ownerId = currentUser.getId();

        if (shortlistRepo.existsByOwnerIdAndName(ownerId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a shortlist named '" + name + "'");
        }

        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Shortlist shortlist = new Shortlist(name, request.notes(), owner);
        return toResponse(shortlistRepo.save(shortlist));
    }

    // READ: managers see their own lists, admins see all
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_MANAGER')")
    @Transactional(readOnly = true)
    public List<ShortlistResponse> getShortlists() {
        List<Shortlist> lists = currentUser.hasRole("ADMIN")
                ? shortlistRepo.findAll()
                : shortlistRepo.findByOwnerId(currentUser.getId());
        return lists.stream().map(this::toResponse).toList();
    }

    // READ one
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_MANAGER')")
    @Transactional(readOnly = true)
    public ShortlistResponse getShortlistById(UUID id) {
        return toResponse(findAccessibleShortlist(id));
    }

    // UPDATE name and notes
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_MANAGER')")
    @Transactional
    public ShortlistResponse updateShortlist(UUID id, ShortlistRequest request) {
        Shortlist shortlist = findAccessibleShortlist(id);
        String name = validateName(request);

        boolean nameChanged = !shortlist.getName().equals(name);
        if (nameChanged && shortlistRepo.existsByOwnerIdAndName(shortlist.getOwner().getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a shortlist named '" + name + "'");
        }

        shortlist.setName(name);
        shortlist.setNotes(request.notes());
        return toResponse(shortlistRepo.save(shortlist));
    }

    // DELETE (join-table rows are removed automatically)
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_MANAGER')")
    @Transactional
    public void deleteShortlist(UUID id) {
        shortlistRepo.delete(findAccessibleShortlist(id));
    }

    // ADD an athlete (US2)
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_MANAGER')")
    @Transactional
    public ShortlistResponse addAthlete(UUID shortlistId, UUID athleteId) {
        Shortlist shortlist = findAccessibleShortlist(shortlistId);
        Athlete athlete = athleteService.getAthleteById(athleteId);   // 404 if missing

        if (!athlete.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Athlete is not active and can't be shortlisted");
        }

        // US2: already on this shortlist → 409 (compare by id)
        boolean alreadyThere = shortlist.getAthletes().stream()
                .anyMatch(a -> a.getId().equals(athleteId));
        if (alreadyThere) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Athlete is already on this shortlist");
        }

        shortlist.getAthletes().add(athlete);
        Shortlist saved = shortlistRepo.save(shortlist);

        // EVENT: athlete receives email + SMS (US2), sent only after the transaction commits
        Map<String, String> data = new HashMap<>();
        data.put("shortlistName", shortlist.getName());
        data.put("athleteId", athlete.getId().toString());
        data.put("athleteName", athlete.getFullName());
        data.put("athleteEmail", athlete.getUser() != null ? athlete.getUser().getEmail() : null);
        data.put("athletePhone", athlete.getContactNumber());
        data.put("managerEmail", shortlist.getOwner().getEmail());
        eventPublisher.publish("shortlist.athleteadded", data);

        return toResponse(saved);
    }

    // REMOVE an athlete
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_MANAGER')")
    @Transactional
    public ShortlistResponse removeAthlete(UUID shortlistId, UUID athleteId) {
        Shortlist shortlist = findAccessibleShortlist(shortlistId);

        boolean removed = shortlist.getAthletes().removeIf(a -> a.getId().equals(athleteId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete is not on this shortlist");
        }
        return toResponse(shortlistRepo.save(shortlist));
    }

    // ---------- helpers ----------

    // 404 if missing; 403 if it belongs to another manager
    private Shortlist findAccessibleShortlist(UUID id) {
        Shortlist shortlist = shortlistRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shortlist not found"));

        boolean isOwner = shortlist.getOwner().getId().equals(currentUser.getId());
        if (!currentUser.hasRole("ADMIN") && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This shortlist belongs to another manager");
        }
        return shortlist;
    }

    private String validateName(ShortlistRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shortlist name is required");
        }
        return request.name().trim();
    }

    // entity → response, built while the session is open (athletes is lazy)
    private ShortlistResponse toResponse(Shortlist s) {
        List<ShortlistAthleteResponse> athletes = s.getAthletes().stream()
                .map(a -> new ShortlistAthleteResponse(
                        a.getId(),
                        a.getAthleteCode(),
                        a.getFullName(),
                        a.getPosition(),
                        a.getSport().getName()))
                .toList();

        return new ShortlistResponse(
                s.getId(),
                s.getName(),
                s.getNotes(),
                s.getOwner().getEmail(),
                athletes.size(),
                athletes);
    }
}