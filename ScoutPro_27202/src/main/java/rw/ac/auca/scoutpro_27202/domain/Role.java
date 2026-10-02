package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    // ADMIN, SCOUT, CLUB_MANAGER, ATHLETE
    @Column(nullable = false, unique = true)
    private String name;

    public Role() {}

    public Role(String name) {
        this.name = name;
    }




    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}