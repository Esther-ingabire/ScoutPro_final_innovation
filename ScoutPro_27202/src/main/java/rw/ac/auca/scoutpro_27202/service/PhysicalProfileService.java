package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Athlete;
import rw.ac.auca.scoutpro_27202.domain.PhysicalProfile;
import rw.ac.auca.scoutpro_27202.repository.PhysicalProfileRepository;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PhysicalProfileService {

    @Autowired
    private PhysicalProfileRepository profileRepo;

    @Autowired
    private AthleteService athleteService;

    // CREATE or UPDATE
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")   // RBAC: scouts measure athletes
    public PhysicalProfile saveOrUpdateProfile(UUID athleteId, PhysicalProfile data) {
        Athlete athlete = athleteService.getAthleteById(athleteId);
        validate(data);

        PhysicalProfile profile = profileRepo.findByAthleteId(athleteId)
                .orElse(new PhysicalProfile());

        profile.setHeightCm(data.getHeightCm());
        profile.setWeightKg(data.getWeightKg());
        profile.setDominantSide(data.getDominantSide());
        profile.setMeasuredAt(data.getMeasuredAt() != null ? data.getMeasuredAt() : LocalDate.now());
        profile.setAthlete(athlete);
        return profileRepo.save(profile);
    }

    // READ
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")   // RBAC
    public PhysicalProfile getProfileByAthlete(UUID athleteId) {
        athleteService.getAthleteById(athleteId);
        return profileRepo.findByAthleteId(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This athlete has no physical profile yet"));
    }

    private void validate(PhysicalProfile p) {
        if (p.getHeightCm() == null || p.getHeightCm() < 50 || p.getHeightCm() > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Height must be between 50 and 250 cm");
        }
        if (p.getWeightKg() == null || p.getWeightKg() < 20 || p.getWeightKg() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Weight must be between 20 and 200 kg");
        }
        if (p.getMeasuredAt() != null && p.getMeasuredAt().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Measurement date can't be in the future");
        }
    }
}