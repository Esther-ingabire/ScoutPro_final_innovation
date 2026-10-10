package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Criterion;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.dto.PageRequests;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.messaging.AuditRecorder;
import rw.ac.auca.scoutpro_27202.messaging.Snapshots;
import rw.ac.auca.scoutpro_27202.repository.AssessmentScoreRepository;
import rw.ac.auca.scoutpro_27202.repository.CriterionRepository;

import java.util.UUID;

@Service
public class CriterionService {

    @Autowired
    private CriterionRepository criterionRepo;

    @Autowired
    private AssessmentScoreRepository scoreRepo;

    @Autowired
    private SportService sportService;

    @Autowired
    private AuditRecorder auditRecorder;

    // CREATE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC: only admins manage criteria
    @CacheEvict(cacheNames = "criteria", allEntries = true)
    public Criterion saveCriterion(UUID sportId, Criterion criterion) {
        Sport sport = sportService.getSportById(sportId);
        validate(criterion);

        String name = criterion.getName().trim();
        if (criterionRepo.findBySportIdAndNameIgnoreCase(sportId, name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion '" + name + "' already exists for this sport");
        }

        criterion.setName(name);
        criterion.setSport(sport);
        Criterion saved = criterionRepo.save(criterion);
        auditRecorder.changed("criterion", saved.getId().toString(), "created", null,
                Snapshots.of("name", saved.getName(), "weight", saved.getWeight(), "sportId", sportId));
        return saved;
    }

    // READ all criteria of one sport (any logged-in user; scouts need these to score)
    @Cacheable(cacheNames = "criteria", key = "#sportId + '-' + #page + '-' + #size")
    public PageResponse<Criterion> getCriteriaBySport(UUID sportId, int page, int size) {
        sportService.getSportById(sportId);
        return PageResponse.of(criterionRepo.findBySportId(sportId, PageRequests.of(page, size)));
    }

    // READ one (any logged-in user)
    public Criterion getCriterionById(UUID id) {
        return criterionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Criterion not found"));
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC
    @CacheEvict(cacheNames = "criteria", allEntries = true)
    public Criterion updateCriterion(UUID id, Criterion newData) {
        Criterion existing = getCriterionById(id);
        String before = Snapshots.of("name", existing.getName(), "weight", existing.getWeight());
        validate(newData);

        String name = newData.getName().trim();
        Criterion sameName = criterionRepo
                .findBySportIdAndNameIgnoreCase(existing.getSport().getId(), name)
                .orElse(null);
        if (sameName != null && !sameName.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion '" + name + "' already exists for this sport");
        }

        existing.setName(name);
        existing.setWeight(newData.getWeight());
        Criterion saved = criterionRepo.save(existing);
        auditRecorder.changed("criterion", saved.getId().toString(), "updated", before,
                Snapshots.of("name", saved.getName(), "weight", saved.getWeight()));
        return saved;
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC
    @CacheEvict(cacheNames = "criteria", allEntries = true)
    public void deleteCriterion(UUID id) {
        if (!criterionRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Criterion not found");
        }
        if (scoreRepo.existsByCriterionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion is already used in assessments and cannot be deleted");
        }
        criterionRepo.deleteById(id);
        auditRecorder.changed("criterion", id.toString(), "deleted", null, null);
    }

    private void validate(Criterion criterion) {
        if (criterion.getName() == null || criterion.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Criterion name is required");
        }
        if (criterion.getWeight() == null || criterion.getWeight() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weight must be greater than 0");
        }
    }
}