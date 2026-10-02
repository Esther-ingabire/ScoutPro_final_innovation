package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.repository.AthleteRepository;
import rw.ac.auca.scoutpro_27202.repository.CriterionRepository;
import rw.ac.auca.scoutpro_27202.repository.SportRepository;
import rw.ac.auca.scoutpro_27202.repository.TeamRepository;

import java.util.List;
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

    // CREATE
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
        return sportRepo.save(sport);
    }

    // READ all
    public List<Sport> getAllSports() {
        return sportRepo.findAll();
    }

    // READ one
    public Sport getSportById(UUID id) {
        return sportRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sport not found"));
    }

    // UPDATE
    public Sport updateSport(UUID id, Sport newData) {
        Sport existing = getSportById(id);   // throws 404 if missing

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
        return sportRepo.save(existing);
    }

    // DELETE
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
    }
}