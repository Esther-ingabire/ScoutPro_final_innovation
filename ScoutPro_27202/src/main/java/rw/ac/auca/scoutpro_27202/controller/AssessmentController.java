package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.dto.AssessmentRequest;
import rw.ac.auca.scoutpro_27202.dto.AssessmentResponse;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.service.AssessmentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @PostMapping("/assessments")
    public ResponseEntity<AssessmentResponse> createAssessment(@RequestBody AssessmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.createAssessment(request));
    }

    @GetMapping("/assessments/ranking")
    public PageResponse<AssessmentResponse> getRanking(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return assessmentService.getRanking(page, size);
    }

    @GetMapping("/assessments/{id}")
    public AssessmentResponse getAssessmentById(@PathVariable UUID id) {
        return assessmentService.getAssessmentById(id);
    }

    @GetMapping("/athletes/{athleteId}/assessments")
    public PageResponse<AssessmentResponse> getAthleteHistory(@PathVariable UUID athleteId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return assessmentService.getAthleteHistory(athleteId, page, size);
    }

    @PutMapping("/assessments/{id}")
    public AssessmentResponse updateAssessment(@PathVariable UUID id, @RequestBody AssessmentRequest request) {
        return assessmentService.updateAssessment(id, request);
    }

    @DeleteMapping("/assessments/{id}")
    public ResponseEntity<Void> deleteAssessment(@PathVariable UUID id) {
        assessmentService.deleteAssessment(id);
        return ResponseEntity.noContent().build();
    }
}