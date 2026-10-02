package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "athletes",
        indexes = {
                @Index(name = "idx_athletes_sport_position", columnList = "sport_id, position"),
                @Index(name = "idx_athletes_dob", columnList = "date_of_birth")
        }
)
public class Athlete extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String athleteCode;     // e.g. ATH-0001

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    private String position;        // e.g. Striker, Point Guard

    private String nationality;

    private String contactNumber;

    private boolean active;

    // FK to Sport: every athlete plays one sport
    @ManyToOne
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    // FK to Team: optional, an athlete may have no team
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // FK to User: optional, only if the athlete has a login account
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public Athlete() {}

    public Athlete(String athleteCode, String fullName, LocalDate dateOfBirth,
                   String position, String nationality, String contactNumber,
                   Sport sport, Team team) {
        this.athleteCode = athleteCode;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.position = position;
        this.nationality = nationality;
        this.contactNumber = contactNumber;
        this.sport = sport;
        this.team = team;
        this.active = true;
    }

    

    public String getAthleteCode() {
        return athleteCode;
    }

    public void setAthleteCode(String athleteCode) {
        this.athleteCode = athleteCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}