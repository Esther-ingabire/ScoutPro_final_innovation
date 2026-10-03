package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.Criterion;
import rw.ac.auca.scoutpro_27202.service.CriterionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CriterionController {

    @Autowired
    private CriterionService criterionService;

    @PostMapping("/sports/{sportId}/criteria")
    public ResponseEntity<Criterion> createCriterion(@PathVariable UUID sportId,
                                                     @RequestBody Criterion criterion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(criterionService.saveCriterion(sportId, criterion));
    }

    @GetMapping("/sports/{sportId}/criteria")
    public List<Criterion> getCriteriaBySport(@PathVariable UUID sportId) {
        return criterionService.getCriteriaBySport(sportId);
    }

    @GetMapping("/criteria/{id}")
    public Criterion getCriterionById(@PathVariable UUID id) {
        return criterionService.getCriterionById(id);
    }

    @PutMapping("/criteria/{id}")
    public Criterion updateCriterion(@PathVariable UUID id, @RequestBody Criterion criterion) {
        return criterionService.updateCriterion(id, criterion);
    }

    @DeleteMapping("/criteria/{id}")
    public ResponseEntity<Void> deleteCriterion(@PathVariable UUID id) {
        criterionService.deleteCriterion(id);
        return ResponseEntity.noContent().build();
    }
}