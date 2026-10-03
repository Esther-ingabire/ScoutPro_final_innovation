package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.service.SportService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sports")
public class SportController {

    @Autowired
    private SportService sportService;

    @PostMapping
    public ResponseEntity<Sport> createSport(@RequestBody Sport sport) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sportService.saveSport(sport));
    }

    @GetMapping
    public List<Sport> getAllSports() {
        return sportService.getAllSports();
    }

    @GetMapping("/{id}")
    public Sport getSportById(@PathVariable UUID id) {
        return sportService.getSportById(id);
    }

    @PutMapping("/{id}")
    public Sport updateSport(@PathVariable UUID id, @RequestBody Sport sport) {
        return sportService.updateSport(id, sport);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSport(@PathVariable UUID id) {
        sportService.deleteSport(id);
        return ResponseEntity.noContent().build();
    }
}