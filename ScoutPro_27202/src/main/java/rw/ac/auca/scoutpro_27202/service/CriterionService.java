package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Criterion;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.repository.AssessmentScoreRepository;
import rw.ac.auca.scoutpro_27202.repository.CriterionRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CriterionService {

    @Autowired
    private CriterionRepository criterionRepo;

    @Autowired
    private AssessmentScoreRepository scoreRepo;

    @Autowired
    private SportService sportService;

    // CREATE
    public Criterion saveCriterion(UUID sportId, Criterion criterion) {
        Sport sport = sportService.getSportById(sportId);   // 404 if sport doesn't exist
        validate(criterion);

        String name = criterion.getName().trim();
        if (criterionRepo.findBySportIdAndNameIgnoreCase(sportId, name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion '" + name + "' already exists for this sport");
        }

        criterion.setName(name);
        criterion.setSport(sport);
        return criterionRepo.save(criterion);
    }

    // READ all criteria of one sport
    public List<Criterion> getCriteriaBySport(UUID sportId) {
        sportService.getSportById(sportId);                 // 404 if sport doesn't exist
        return criterionRepo.findBySportId(sportId);
    }

    // READ one
    public Criterion getCriterionById(UUID id) {
        return criterionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Criterion not found"));
    }

    // UPDATE (name and weight only, a criterion can't move to another sport)
    public Criterion updateCriterion(UUID id, Criterion newData) {
        Criterion existing = getCriterionById(id);
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
        return criterionRepo.save(existing);
    }

    // DELETE
    public void deleteCriterion(UUID id) {
        if (!criterionRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Criterion not found");
        }
        if (scoreRepo.existsByCriterionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Criterion is already used in assessments and cannot be deleted");
        }
        criterionRepo.deleteById(id);
    }

    // shared checks for create and update
    private void validate(Criterion criterion) {
        if (criterion.getName() == null || criterion.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Criterion name is required");
        }
        if (criterion.getWeight() == null || criterion.getWeight() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weight must be greater than 0");
        }
    }
}