package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.document.MediaClip;
import rw.ac.auca.scoutpro_27202.document.ScoutingReport;
import rw.ac.auca.scoutpro_27202.domain.Assessment;
import rw.ac.auca.scoutpro_27202.dto.PageRequests;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.dto.ReportFile;
import rw.ac.auca.scoutpro_27202.dto.ScoutingReportRequest;
import rw.ac.auca.scoutpro_27202.messaging.AuditRecorder;
import rw.ac.auca.scoutpro_27202.messaging.Snapshots;
import rw.ac.auca.scoutpro_27202.repository.AssessmentRepository;
import rw.ac.auca.scoutpro_27202.repository.ScoutingReportRepository;
import rw.ac.auca.scoutpro_27202.security.CurrentUser;
import rw.ac.auca.scoutpro_27202.service.AthleteService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ScoutingReportService {

    @Autowired
    private ScoutingReportRepository reportRepo;

    @Autowired
    private AssessmentRepository assessmentRepo;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private AthleteService athleteService;

    @Autowired
    private AuditRecorder auditRecorder;

    // CREATE: only the scout who made the assessment
    @PreAuthorize("hasRole('SCOUT')")
    public ScoutingReport createReport(UUID assessmentId, ScoutingReportRequest request) {
        Assessment assessment = findAssessment(assessmentId);
        checkIsAuthor(assessment);

        if (reportRepo.existsByAssessmentId(assessmentId.toString())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This assessment already has a report; update it instead");
        }

        ScoutingReport report = new ScoutingReport();
        // the links come from PostgreSQL, never from the client
        report.setAssessmentId(assessment.getId().toString());
        report.setAthleteId(assessment.getAthlete().getId().toString());
        report.setScoutId(assessment.getScout().getId().toString());
        report.setCreatedAt(Instant.now());
        applyRequest(report, request);

        ScoutingReport saved = reportRepo.save(report);
        auditRecorder.changed("scoutingReport", saved.getId(), "created", null,
                Snapshots.of("assessmentId", saved.getAssessmentId(), "summary", saved.getSummary()));
        return saved;
    }

    // PDF download of one report. Same permission as reading it.
    // An athlete receives their own summary. Staff receive the scores and the full report.
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER', 'ATHLETE')")
    public ReportFile pdf(UUID assessmentId) {
        ScoutingReport report = getByAssessment(assessmentId);
        Assessment assessment = assessmentRepo.findDetailedById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        String safe = assessment.getAthlete().getFullName().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (safe.isBlank()) {
            safe = "report";
        }
        return new ReportFile(ReportPdf.write(assessment, report, !athleteOnly()), "scoutpro-" + safe + ".pdf");
    }

    // READ the report of one assessment. Athletes receive the summary only.
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER', 'ATHLETE')")
    public ScoutingReport getByAssessment(UUID assessmentId) {
        ScoutingReport report = loadReport(assessmentId);
        athleteService.getAthleteById(UUID.fromString(report.getAthleteId()));
        return visible(report);
    }

    // READ all reports about one athlete, newest first
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER', 'ATHLETE')")
    public PageResponse<ScoutingReport> getByAthlete(UUID athleteId, int page, int size) {
        athleteService.getAthleteById(athleteId);
        Page<ScoutingReport> result = reportRepo.findByAthleteIdOrderByCreatedAtDesc(
                athleteId.toString(), PageRequests.of(page, size));
        return PageResponse.of(result.getContent().stream().map(this::visible).toList(), result);
    }

    // SEARCH reports by words in summary or tags, e.g. "winger"
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    public PageResponse<ScoutingReport> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text is required");
        }
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matching(query);
        return PageResponse.of(reportRepo.findAllBy(criteria, PageRequests.of(page, size)));
    }

    // UPDATE: the author, or an admin
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    public ScoutingReport updateReport(UUID assessmentId, ScoutingReportRequest request) {
        Assessment assessment = findAssessment(assessmentId);
        if (!currentUser.hasRole("ADMIN")) {
            checkIsAuthor(assessment);
        }
        ScoutingReport report = loadReport(assessmentId);
        applyRequest(report, request);
        ScoutingReport saved = reportRepo.save(report);
        auditRecorder.changed("scoutingReport", saved.getId(), "updated", null,
                Snapshots.of("summary", saved.getSummary()));
        return saved;
    }

    // DELETE: the author, or an admin
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    public void deleteReport(UUID assessmentId) {
        Assessment assessment = findAssessment(assessmentId);
        if (!currentUser.hasRole("ADMIN")) {
            checkIsAuthor(assessment);
        }
        ScoutingReport report = loadReport(assessmentId);
        reportRepo.delete(report);
        auditRecorder.changed("scoutingReport", report.getId(), "deleted", null, null);
    }

    // ---------- helpers ----------

    private ScoutingReport loadReport(UUID assessmentId) {
        return reportRepo.findByAssessmentId(assessmentId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No report for this assessment"));
    }

    // Staff see the full document. An athlete account sees the summary only.
    private ScoutingReport visible(ScoutingReport report) {
        if (!athleteOnly()) {
            return report;
        }
        ScoutingReport summary = new ScoutingReport();
        summary.setId(report.getId());
        summary.setAssessmentId(report.getAssessmentId());
        summary.setAthleteId(report.getAthleteId());
        summary.setScoutId(report.getScoutId());
        summary.setSummary(report.getSummary());
        summary.setCreatedAt(report.getCreatedAt());
        summary.setUpdatedAt(report.getUpdatedAt());
        return summary;
    }

    private boolean athleteOnly() {
        return currentUser.hasRole("ATHLETE")
                && !currentUser.hasRole("ADMIN")
                && !currentUser.hasRole("SCOUT")
                && !currentUser.hasRole("CLUB_MANAGER");
    }

    private Assessment findAssessment(UUID id) {
        return assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
    }

    private void checkIsAuthor(Assessment assessment) {
        boolean isAuthor = assessment.getScout().getUser().getId().equals(currentUser.getId());
        if (!isAuthor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the scout who made the assessment can write its report");
        }
    }

    // validate and copy the request onto the document
    private void applyRequest(ScoutingReport report, ScoutingReportRequest request) {
        if (request.summary() == null || request.summary().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summary is required");
        }
        if (request.summary().length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summary is too long (max 5000 characters)");
        }

        List<MediaClip> media = request.media() != null ? request.media() : new ArrayList<>();
        for (MediaClip clip : media) {
            if (clip.url() == null || !clip.url().startsWith("http")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every media item needs a valid http(s) url");
            }
            if (clip.startSec() != null && clip.startSec() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startSec can't be negative");
            }
        }

        report.setSummary(request.summary().trim());
        report.setStrengths(clean(request.strengths()));
        report.setWeaknesses(clean(request.weaknesses()));
        report.setTags(clean(request.tags()).stream().map(String::toLowerCase).distinct().toList());
        report.setMedia(media);
        report.setUpdatedAt(Instant.now());
    }

    // null → empty list; trims and drops blank entries
    private List<String> clean(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
    }
}