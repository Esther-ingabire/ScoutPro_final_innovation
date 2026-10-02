package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "shortlists",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"})
)
public class Shortlist extends BaseEntity {

    @Column(nullable = false)
    private String name;        // e.g. "U-20 Strikers 2026"

    @Column(length = 1000)
    private String notes;

    // FK to User: the club manager who owns this shortlist
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // join table shortlist_athletes(shortlist_id, athlete_id)
    @ManyToMany
    @JoinTable(
            name = "shortlist_athletes",
            joinColumns = @JoinColumn(name = "shortlist_id"),
            inverseJoinColumns = @JoinColumn(name = "athlete_id")
    )
    private Set<Athlete> athletes = new HashSet<>();

    public Shortlist() {}

    public Shortlist(String name, String notes, User owner) {
        this.name = name;
        this.notes = notes;
        this.owner = owner;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Set<Athlete> getAthletes() {
        return athletes;
    }

    public void setAthletes(Set<Athlete> athletes) {
        this.athletes = athletes;
    }
}