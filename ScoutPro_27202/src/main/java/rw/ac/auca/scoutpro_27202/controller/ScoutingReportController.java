package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.document.ScoutingReport;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.dto.ReportFile;
import rw.ac.auca.scoutpro_27202.dto.ScoutingReportRequest;
import rw.ac.auca.scoutpro_27202.service.ScoutingReportService;

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

    @GetMapping(value = "/assessments/{assessmentId}/report.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID assessmentId) {
        ReportFile file = reportService.pdf(assessmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.bytes());
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
    public PageResponse<ScoutingReport> getAthleteReports(@PathVariable UUID athleteId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return reportService.getByAthlete(athleteId, page, size);
    }

    @GetMapping("/reports/search")
    public PageResponse<ScoutingReport> search(@RequestParam String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return reportService.search(q, page, size);
    }
}