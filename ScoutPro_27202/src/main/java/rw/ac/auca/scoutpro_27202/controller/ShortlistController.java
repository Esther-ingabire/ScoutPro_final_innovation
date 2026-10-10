package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.dto.ShortlistRequest;
import rw.ac.auca.scoutpro_27202.dto.ShortlistResponse;
import rw.ac.auca.scoutpro_27202.service.ShortlistService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shortlists")
public class ShortlistController {

    @Autowired
    private ShortlistService shortlistService;

    @PostMapping
    public ResponseEntity<ShortlistResponse> createShortlist(@RequestBody ShortlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortlistService.createShortlist(request));
    }

    @GetMapping
    public PageResponse<ShortlistResponse> getShortlists(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return shortlistService.getShortlists(page, size);
    }

    @GetMapping("/{id}")
    public ShortlistResponse getShortlistById(@PathVariable UUID id) {
        return shortlistService.getShortlistById(id);
    }

    @PutMapping("/{id}")
    public ShortlistResponse updateShortlist(@PathVariable UUID id, @RequestBody ShortlistRequest request) {
        return shortlistService.updateShortlist(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShortlist(@PathVariable UUID id) {
        shortlistService.deleteShortlist(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/athletes/{athleteId}")
    public ShortlistResponse addAthlete(@PathVariable UUID id, @PathVariable UUID athleteId) {
        return shortlistService.addAthlete(id, athleteId);
    }

    @DeleteMapping("/{id}/athletes/{athleteId}")
    public ShortlistResponse removeAthlete(@PathVariable UUID id, @PathVariable UUID athleteId) {
        return shortlistService.removeAthlete(id, athleteId);
    }
}