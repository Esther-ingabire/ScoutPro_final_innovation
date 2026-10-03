package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "assessments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"scout_id", "athlete_id", "assessment_date"}
        ),
        indexes = {
                @Index(name = "idx_assessments_overall_score", columnList = "overall_score DESC"),
                @Index(name = "idx_assessments_athlete_date", columnList = "athlete_id, assessment_date DESC")
        }
)
public class Assessment extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String assessmentCode;      // e.g. ASM-0001

    // calculated by the service as a weighted average, never typed by the user
    private Double overallScore;

    @Column(nullable = false)
    private LocalDate assessmentDate;

    @Column(length = 1000)
    private String remarks;

    // FK to Athlete
    @ManyToOne
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    // FK to Scout
    @ManyToOne
    @JoinColumn(name = "scout_id", nullable = false)
    private Scout scout;

    // one score per criterion
    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssessmentScore> scores = new ArrayList<>();

    public Assessment() {}

    public Assessment(String assessmentCode, LocalDate assessmentDate, String remarks,
                      Athlete athlete, Scout scout) {
        this.assessmentCode = assessmentCode;
        this.assessmentDate = assessmentDate;
        this.remarks = remarks;
        this.athlete = athlete;
        this.scout = scout;
    }

    // keeps both sides of the relationship in sync
    public void addScore(AssessmentScore score) {
        scores.add(score);
        score.setAssessment(this);
    }

    // generate getters and setters

    public String getAssessmentCode() {
        return assessmentCode;
    }

    public void setAssessmentCode(String assessmentCode) {
        this.assessmentCode = assessmentCode;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(LocalDate assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public Scout getScout() {
        return scout;
    }

    public void setScout(Scout scout) {
        this.scout = scout;
    }

    public List<AssessmentScore> getScores() {
        return scores;
    }

    public void setScores(List<AssessmentScore> scores) {
        this.scores = scores;
    }
}