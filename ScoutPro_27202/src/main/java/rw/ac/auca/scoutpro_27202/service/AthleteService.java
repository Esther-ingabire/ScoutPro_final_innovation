package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Athlete;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.domain.Team;
import rw.ac.auca.scoutpro_27202.repository.AssessmentRepository;
import rw.ac.auca.scoutpro_27202.repository.AthleteRepository;
import rw.ac.auca.scoutpro_27202.repository.PhysicalProfileRepository;
import rw.ac.auca.scoutpro_27202.repository.ShortlistRepository;

import java.time.LocalDate;
import java.util.List;
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

    // CREATE
    public Athlete saveAthlete(UUID sportId, UUID teamId, Athlete athlete) {
        Sport sport = sportService.getSportById(sportId);   // 404 if sport doesn't exist
        validate(athlete);

        String code = athlete.getAthleteCode().trim().toUpperCase();
        if (athleteRepo.existsByAthleteCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Athlete code already exists");
        }

        athlete.setAthleteCode(code);
        athlete.setSport(sport);
        athlete.setTeam(resolveTeam(teamId, sportId));
        athlete.setActive(true);                            // new athletes are always active
        return athleteRepo.save(athlete);
    }

    // READ all
    public List<Athlete> getAllAthletes() {
        return athleteRepo.findAll();
    }

    // READ one
    public Athlete getAthleteById(UUID id) {
        return athleteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));
    }

    // UPDATE (sport can't change; teamId = null removes the athlete from their team)
    public Athlete updateAthlete(UUID id, UUID teamId, Athlete newData) {
        Athlete existing = getAthleteById(id);              // 404 if missing
        validate(newData);

        String code = newData.getAthleteCode().trim().toUpperCase();
        Athlete sameCode = athleteRepo.findByAthleteCode(code).orElse(null);
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Athlete code already exists");
        }

        existing.setAthleteCode(code);
        existing.setFullName(newData.getFullName());
        existing.setDateOfBirth(newData.getDateOfBirth());
        existing.setPosition(newData.getPosition());
        existing.setNationality(newData.getNationality());
        existing.setContactNumber(newData.getContactNumber());
        existing.setTeam(resolveTeam(teamId, existing.getSport().getId()));
        return athleteRepo.save(existing);
    }

    // DEACTIVATE (the safe alternative to deleting)
    public Athlete deactivateAthlete(UUID id) {
        Athlete athlete = getAthleteById(id);
        athlete.setActive(false);
        return athleteRepo.save(athlete);
    }

    // DELETE (only for athletes with no history)
    @Transactional
    public void deleteAthlete(UUID id) {
        if (!athleteRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found");
        }
        if (assessmentRepo.existsByAthleteId(id) || shortlistRepo.existsByAthletes_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Athlete has assessments or is shortlisted; deactivate instead");
        }
        profileRepo.findByAthleteId(id).ifPresent(profileRepo::delete);  // remove profile first
        athleteRepo.deleteById(id);
    }

    // ---------- helpers ----------

    // the team (if given) must exist AND play the athlete's sport
    private Team resolveTeam(UUID teamId, UUID sportId) {
        if (teamId == null) {
            return null;                                     // athlete without a team
        }
        Team team = teamService.getTeamById(teamId);         // 404 if team doesn't exist
        if (!team.getSport().getId().equals(sportId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Team plays a different sport than the athlete");
        }
        return team;
    }

    // shared checks for create and update
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