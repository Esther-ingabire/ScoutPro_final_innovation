package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "assessment_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessment_id", "criterion_id"}),
        check = @CheckConstraint(name = "ck_score_range", constraint = "score BETWEEN 0 AND 100")
)
public class AssessmentScore extends BaseEntity {

    @Column(nullable = false)
    private Double score;           // 0 to 100

    // FK to Assessment
    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    // FK to Criterion
    @ManyToOne
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criterion criterion;

    public AssessmentScore() {}

    public AssessmentScore(Double score, Criterion criterion) {
        this.score = score;
        this.criterion = criterion;
    }

    // generate getters and setters

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public Criterion getCriterion() {
        return criterion;
    }

    public void setCriterion(Criterion criterion) {
        this.criterion = criterion;
    }
}