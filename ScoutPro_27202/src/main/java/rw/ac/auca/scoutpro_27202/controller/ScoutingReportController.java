package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.document.ScoutingReport;
import rw.ac.auca.scoutpro_27202.dto.ScoutingReportRequest;
import rw.ac.auca.scoutpro_27202.service.ScoutingReportService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ScoutingReportController {

    @Autowired
    private ScoutingReportService reportService;

    @PostMapping("/assessments/{assessmentId}/report")
    public ResponseEntity<ScoutingReport> createReport(@PathVariable UUID assessmentId,
                                                       @RequestBody ScoutingReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(assessmentId, request));
    }

    @GetMapping("/assessments/{assessmentId}/report")
    public ScoutingReport getReport(@PathVariable UUID assessmentId) {
        return reportService.getByAssessment(assessmentId);
    }

    @PutMapping("/assessments/{assessmentId}/report")
    public ScoutingReport updateReport(@PathVariable UUID assessmentId,
                                       @RequestBody ScoutingReportRequest request) {
        return reportService.updateReport(assessmentId, request);
    }

    @DeleteMapping("/assessments/{assessmentId}/report")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID assessmentId) {
        reportService.deleteReport(assessmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/athletes/{athleteId}/reports")
    public List<ScoutingReport> getAthleteReports(@PathVariable UUID athleteId) {
        return reportService.getByAthlete(athleteId);
    }

    @GetMapping("/reports/search")
    public List<ScoutingReport> search(@RequestParam String q) {
        return reportService.search(q);
    }
}