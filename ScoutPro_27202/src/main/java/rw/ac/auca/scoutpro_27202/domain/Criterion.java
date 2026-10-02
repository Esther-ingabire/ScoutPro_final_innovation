package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "criteria")
public class Criterion extends BaseEntity {

    @Column(nullable = false)
    private String name;        // e.g. Speed, Passing, Stamina

    @Column(nullable = false)
    private Double weight;      // how much this criterion counts

    // FK to Sport
    @ManyToOne
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    public Criterion() {}

    public Criterion(String name, Double weight, Sport sport) {
        this.name = name;
        this.weight = weight;
        this.sport = sport;
    }

    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }
}