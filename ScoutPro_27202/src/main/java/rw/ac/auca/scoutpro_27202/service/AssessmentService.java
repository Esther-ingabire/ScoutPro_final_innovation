package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.*;
import rw.ac.auca.scoutpro_27202.dto.AssessmentRequest;
import rw.ac.auca.scoutpro_27202.dto.AssessmentResponse;
import rw.ac.auca.scoutpro_27202.dto.ScoreRequest;
import rw.ac.auca.scoutpro_27202.dto.ScoreResponse;
import rw.ac.auca.scoutpro_27202.repository.AssessmentRepository;
import rw.ac.auca.scoutpro_27202.repository.CriterionRepository;
import rw.ac.auca.scoutpro_27202.security.CurrentUser;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    @Autowired
    private AssessmentRepository assessmentRepo;

    @Autowired
    private CriterionRepository criterionRepo;

    @Autowired
    private AthleteService athleteService;

    @Autowired
    private ScoutService scoutService;

    @Autowired
    private CurrentUser currentUser;

    // CREATE: only scouts record assessments (US1)
    @PreAuthorize("hasRole('SCOUT')")
    @Transactional
    public AssessmentResponse createAssessment(AssessmentRequest request) {
        // 1. the scout is whoever is logged in
        Scout scout = scoutService.getMyScoutProfile();
        if (!scout.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your scout profile is deactivated");
        }

        // 2. the athlete must exist and be active
        if (request.athleteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "athleteId is required");
        }
        Athlete athlete = athleteService.getAthleteById(request.athleteId());
        if (!athlete.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Athlete is not active");
        }

        // 3. date defaults to today, can't be in the future
        LocalDate date = request.assessmentDate() != null ? request.assessmentDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assessment date can't be in the future");
        }

        // 4. same-day rule (US1): one assessment per scout, per athlete, per day
        if (assessmentRepo.existsByScoutIdAndAthleteIdAndAssessmentDate(scout.getId(), athlete.getId(), date)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "You already assessed this athlete on " + date);
        }

        // 5. scores must match the athlete's sport's criteria exactly
        List<Criterion> criteria = criterionRepo.findBySportId(athlete.getSport().getId());
        Map<UUID, Double> scores = validateScores(request.scores(), criteria);

        // 6. build the assessment with one score per criterion
        Assessment assessment = new Assessment(generateCode(), date, request.remarks(), athlete, scout);
        for (Criterion criterion : criteria) {
            assessment.addScore(new AssessmentScore(scores.get(criterion.getId()), criterion));
        }
        assessment.setOverallScore(calculateOverall(scores, criteria));

        // 7. save assessment + all scores in one go (cascade)
        try {
            assessmentRepo.saveAndFlush(assessment);
        } catch (DataIntegrityViolationException e) {
            // two identical requests at the same instant: the DB unique constraint caught the second
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "You already assessed this athlete on " + date);
        }
        return toResponse(assessment);
    }

    // READ one
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    @Transactional(readOnly = true)
    public AssessmentResponse getAssessmentById(UUID id) {
        return toResponse(findAssessment(id));
    }

    // READ an athlete's history, newest first
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAthleteHistory(UUID athleteId, int page, int size) {
        athleteService.getAthleteById(athleteId);   // 404 if the athlete doesn't exist
        return assessmentRepo
                .findByAthleteIdOrderByAssessmentDateDesc(athleteId, PageRequest.of(page, Math.min(size, 100)))
                .getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    // READ ranking: highest overall score first (simple version)
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')")
    @Transactional(readOnly = true)
    public List<AssessmentResponse> getRanking(int page, int size) {
        return assessmentRepo
                .findAllByOrderByOverallScoreDesc(PageRequest.of(page, Math.min(size, 100)))
                .getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    // UPDATE remarks and scores: admin, or the scout who made it
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    @Transactional
    public AssessmentResponse updateAssessment(UUID id, AssessmentRequest request) {
        Assessment assessment = findAssessment(id);
        checkCanModify(assessment);

        List<Criterion> criteria = criterionRepo.findBySportId(assessment.getAthlete().getSport().getId());
        Map<UUID, Double> scores = validateScores(request.scores(), criteria);

        // change existing score rows in place; add rows only for criteria added since
        Map<UUID, AssessmentScore> existing = new HashMap<>();
        for (AssessmentScore s : assessment.getScores()) {
            existing.put(s.getCriterion().getId(), s);
        }
        for (Criterion criterion : criteria) {
            AssessmentScore row = existing.get(criterion.getId());
            if (row != null) {
                row.setScore(scores.get(criterion.getId()));
            } else {
                assessment.addScore(new AssessmentScore(scores.get(criterion.getId()), criterion));
            }
        }

        assessment.setRemarks(request.remarks());
        assessment.setOverallScore(calculateOverall(scores, criteria));
        return toResponse(assessmentRepo.save(assessment));
    }

    // DELETE: admin, or the scout who made it (scores are deleted by cascade)
    @PreAuthorize("hasAnyRole('ADMIN', 'SCOUT')")
    @Transactional
    public void deleteAssessment(UUID id) {
        Assessment assessment = findAssessment(id);
        checkCanModify(assessment);
        assessmentRepo.delete(assessment);
    }

    // ---------- helpers ----------

    private Assessment findAssessment(UUID id) {
        return assessmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
    }

    // ownership: admins can modify any assessment, scouts only their own
    private void checkCanModify(Assessment assessment) {
        if (currentUser.hasRole("ADMIN")) {
            return;
        }
        boolean isOwner = assessment.getScout().getUser().getId().equals(currentUser.getId());
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own assessments");
        }
    }

    // every criterion of the sport scored exactly once, 0–100, nothing from other sports
    private Map<UUID, Double> validateScores(List<ScoreRequest> input, List<Criterion> criteria) {
        if (criteria.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "This sport has no criteria yet; an admin must add them first");
        }
        if (input == null || input.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scores are required");
        }

        Set<UUID> validIds = criteria.stream().map(Criterion::getId).collect(Collectors.toSet());
        Map<UUID, Double> scores = new HashMap<>();

        for (ScoreRequest s : input) {
            if (s.criterionId() == null || !validIds.contains(s.criterionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Criterion " + s.criterionId() + " does not belong to this athlete's sport");
            }
            if (s.score() == null || s.score() < 0 || s.score() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each score must be between 0 and 100");
            }
            if (scores.put(s.criterionId(), s.score()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A criterion was scored more than once");
            }
        }

        // US1: name the missing criteria
        List<String> missing = criteria.stream()
                .filter(c -> !scores.containsKey(c.getId()))
                .map(Criterion::getName)
                .toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing scores for: " + String.join(", ", missing));
        }
        return scores;
    }

    // FR5: overall = Σ(score × weight) ÷ Σ(weight), rounded to 2 decimals
    private double calculateOverall(Map<UUID, Double> scores, List<Criterion> criteria) {
        double weightedSum = 0;
        double totalWeight = 0;
        for (Criterion c : criteria) {
            weightedSum += scores.get(c.getId()) * c.getWeight();
            totalWeight += c.getWeight();
        }
        return Math.round(weightedSum / totalWeight * 100) / 100.0;
    }

    // e.g. ASM-3F2A9C1E
    private String generateCode() {
        return "ASM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // entity → response DTO, built while the DB session is still open
    private AssessmentResponse toResponse(Assessment a) {
        List<ScoreResponse> scoreList = a.getScores().stream()
                .map(s -> new ScoreResponse(
                        s.getCriterion().getId(),
                        s.getCriterion().getName(),
                        s.getCriterion().getWeight(),
                        s.getScore()))
                .toList();

        return new AssessmentResponse(
                a.getId(),
                a.getAssessmentCode(),
                a.getAthlete().getId(),
                a.getAthlete().getFullName(),
                a.getScout().getId(),
                a.getScout().getFullName(),
                a.getAssessmentDate(),
                a.getOverallScore(),
                a.getRemarks(),
                scoreList);
    }
}