package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "teams",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "sport_id"})
)
public class Team extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamLevel level;

    // FK to Sport
    @ManyToOne
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    public Team() {}

    public Team(String name, String city, TeamLevel level, Sport sport) {
        this.name = name;
        this.city = city;
        this.level = level;
        this.sport = sport;
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public TeamLevel getLevel() {
        return level;
    }

    public void setLevel(TeamLevel level) {
        this.level = level;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }
}