package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.document.MediaClip;
import rw.ac.auca.scoutpro_27202.document.ScoutingReport;
import rw.ac.auca.scoutpro_27202.domain.Assessment;
import rw.ac.auca.scoutpro_27202.dto.ScoutingReportRequest;
import rw.ac.auca.scoutpro_27202.repository.AssessmentRepository;
import rw.ac.auca.scoutpro_27202.repository.ScoutingReportRepository;
import rw.ac.auca.scoutpro_27202.security.CurrentUser;

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

        return reportRepo.save(report);
    }

    // READ the report of one assessment
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    public ScoutingReport getByAssessment(UUID assessmentId) {
        return reportRepo.findByAssessmentId(assessmentId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No report for this assessment"));
    }

    // READ all reports about one athlete, newest first
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    public List<ScoutingReport> getByAthlete(UUID athleteId) {
        return reportRepo.findByAthleteIdOrderByCreatedAtDesc(athleteId.toString());
    }

    // SEARCH reports by words in summary or tags, e.g. "winger"
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    public List<ScoutingReport> search(String query) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text is required");
        }
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matching(query);
        return reportRepo.findAllBy(criteria);
    }

    // UPDATE: the author, or an admin
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    public ScoutingReport updateReport(UUID assessmentId, ScoutingReportRequest request) {
        Assessment assessment = findAssessment(assessmentId);
        if (!currentUser.hasRole("ADMIN")) {
            checkIsAuthor(assessment);
        }
        ScoutingReport report = getByAssessment(assessmentId);
        applyRequest(report, request);
        return reportRepo.save(report);
    }

    // DELETE: the author, or an admin
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    public void deleteReport(UUID assessmentId) {
        Assessment assessment = findAssessment(assessmentId);
        if (!currentUser.hasRole("ADMIN")) {
            checkIsAuthor(assessment);
        }
        ScoutingReport report = getByAssessment(assessmentId);
        reportRepo.delete(report);
    }

    // ---------- helpers ----------

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