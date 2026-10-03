package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.Athlete;
import rw.ac.auca.scoutpro_27202.service.AthleteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AthleteController {

    @Autowired
    private AthleteService athleteService;

    @PostMapping("/sports/{sportId}/athletes")
    public ResponseEntity<Athlete> createAthlete(@PathVariable UUID sportId,
                                                 @RequestParam(required = false) UUID teamId,
                                                 @RequestBody Athlete athlete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(athleteService.saveAthlete(sportId, teamId, athlete));
    }

    @GetMapping("/athletes")
    public List<Athlete> getAllAthletes() {
        return athleteService.getAllAthletes();
    }

    @GetMapping("/athletes/{id}")
    public Athlete getAthleteById(@PathVariable UUID id) {
        return athleteService.getAthleteById(id);
    }

    @PutMapping("/athletes/{id}")
    public Athlete updateAthlete(@PathVariable UUID id,
                                 @RequestParam(required = false) UUID teamId,
                                 @RequestBody Athlete athlete) {
        return athleteService.updateAthlete(id, teamId, athlete);
    }

    @PatchMapping("/athletes/{id}/deactivate")
    public Athlete deactivateAthlete(@PathVariable UUID id) {
        return athleteService.deactivateAthlete(id);
    }

    @DeleteMapping("/athletes/{id}")
    public ResponseEntity<Void> deleteAthlete(@PathVariable UUID id) {
        athleteService.deleteAthlete(id);
        return ResponseEntity.noContent().build();
    }
}