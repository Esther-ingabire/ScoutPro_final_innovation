package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.Scout;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.service.ScoutService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ScoutController {

    @Autowired
    private ScoutService scoutService;

    @PostMapping("/users/{userId}/scout")
    public ResponseEntity<Scout> createScout(@PathVariable UUID userId, @RequestBody Scout scout) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scoutService.saveScout(userId, scout));
    }

    @GetMapping("/scouts")
    public PageResponse<Scout> getAllScouts(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return scoutService.getAllScouts(page, size);
    }

    @GetMapping("/scouts/me")
    public Scout getMyProfile() {
        return scoutService.getMyScoutProfile();
    }

    @GetMapping("/scouts/{id}")
    public Scout getScoutById(@PathVariable UUID id) {
        return scoutService.getScoutById(id);
    }

    @PutMapping("/scouts/{id}")
    public Scout updateScout(@PathVariable UUID id, @RequestBody Scout scout) {
        return scoutService.updateScout(id, scout);
    }

    @PatchMapping("/scouts/{id}/deactivate")
    public Scout deactivateScout(@PathVariable UUID id) {
        return scoutService.deactivateScout(id);
    }

    @DeleteMapping("/scouts/{id}")
    public ResponseEntity<Void> deleteScout(@PathVariable UUID id) {
        scoutService.deleteScout(id);
        return ResponseEntity.noContent().build();
    }
}