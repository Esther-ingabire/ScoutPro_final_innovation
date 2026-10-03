package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "physical_profiles")
public class PhysicalProfile extends BaseEntity {

    private Double heightCm;

    private Double weightKg;

    @Enumerated(EnumType.STRING)
    private DominantSide dominantSide;   // dominant foot or hand

    private LocalDate measuredAt;

    // FK to Athlete: one profile per athlete
    @OneToOne
    @JoinColumn(name = "athlete_id", nullable = false, unique = true)
    private Athlete athlete;

    public PhysicalProfile() {}

    public PhysicalProfile(Double heightCm, Double weightKg, DominantSide dominantSide,
                           LocalDate measuredAt, Athlete athlete) {
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.dominantSide = dominantSide;
        this.measuredAt = measuredAt;
        this.athlete = athlete;
    }



    public Double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public DominantSide getDominantSide() {
        return dominantSide;
    }

    public void setDominantSide(DominantSide dominantSide) {
        this.dominantSide = dominantSide;
    }

    public LocalDate getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalDate measuredAt) {
        this.measuredAt = measuredAt;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }
}