package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.dto.PageRequests;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.messaging.AuditRecorder;
import rw.ac.auca.scoutpro_27202.messaging.Snapshots;
import rw.ac.auca.scoutpro_27202.repository.AthleteRepository;
import rw.ac.auca.scoutpro_27202.repository.CriterionRepository;
import rw.ac.auca.scoutpro_27202.repository.SportRepository;
import rw.ac.auca.scoutpro_27202.repository.TeamRepository;

import java.util.UUID;

@Service
public class SportService {

    @Autowired
    private SportRepository sportRepo;

    @Autowired
    private CriterionRepository criterionRepo;

    @Autowired
    private TeamRepository teamRepo;

    @Autowired
    private AthleteRepository athleteRepo;

    @Autowired
    private AuditRecorder auditRecorder;

    // CREATE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC: only admins manage sports
    @CacheEvict(cacheNames = {"sports", "criteria"}, allEntries = true)
    public Sport saveSport(Sport sport) {
        if (sport.getName() == null || sport.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sport name is required");
        }
        if (sport.getCode() == null || sport.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sport code is required");
        }

        sport.setCode(sport.getCode().trim().toUpperCase());

        if (sportRepo.existsByCode(sport.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sport code already exists");
        }
        Sport saved = sportRepo.save(sport);
        auditRecorder.changed("sport", saved.getId().toString(), "created", null,
                Snapshots.of("name", saved.getName(), "code", saved.getCode()));
        return saved;
    }

    // READ all (any logged-in user). Cached: the list changes only when an admin edits sports.
    @Cacheable(cacheNames = "sports", key = "#page + '-' + #size")
    public PageResponse<Sport> getAllSports(int page, int size) {
        return PageResponse.of(sportRepo.findAll(PageRequests.of(page, size)));
    }

    // READ one (any logged-in user)
    public Sport getSportById(UUID id) {
        return sportRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sport not found"));
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC
    @CacheEvict(cacheNames = {"sports", "criteria"}, allEntries = true)
    public Sport updateSport(UUID id, Sport newData) {
        Sport existing = getSportById(id);
        String before = Snapshots.of("name", existing.getName(), "code", existing.getCode());

        if (newData.getName() == null || newData.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sport name is required");
        }
        if (newData.getCode() == null || newData.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sport code is required");
        }

        String newCode = newData.getCode().trim().toUpperCase();
        Sport sameCode = sportRepo.findByCode(newCode).orElse(null);
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sport code already exists");
        }

        existing.setName(newData.getName());
        existing.setCode(newCode);
        Sport saved = sportRepo.save(existing);
        auditRecorder.changed("sport", saved.getId().toString(), "updated", before,
                Snapshots.of("name", saved.getName(), "code", saved.getCode()));
        return saved;
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC
    @CacheEvict(cacheNames = {"sports", "criteria"}, allEntries = true)
    public void deleteSport(UUID id) {
        if (!sportRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sport not found");
        }
        if (criterionRepo.existsBySportId(id) || teamRepo.existsBySportId(id)
                || athleteRepo.existsBySportId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sport is used by criteria, teams or athletes");
        }
        sportRepo.deleteById(id);
        auditRecorder.changed("sport", id.toString(), "deleted", null, null);
    }
}