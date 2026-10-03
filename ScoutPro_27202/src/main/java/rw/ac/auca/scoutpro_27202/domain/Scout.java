package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "scouts")
public class Scout extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String scoutCode;       // e.g. SC-0001

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;

    private String organization;    // club or academy they work for

    private boolean active;

    // FK to User: every scout must have a login account
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public Scout() {}

    public Scout(String scoutCode, String fullName, String email,
                 String phoneNumber, String organization, User user) {
        this.scoutCode = scoutCode;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.organization = organization;
        this.user = user;
        this.active = true;
    }



    public String getScoutCode() {
        return scoutCode;
    }

    public void setScoutCode(String scoutCode) {
        this.scoutCode = scoutCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}