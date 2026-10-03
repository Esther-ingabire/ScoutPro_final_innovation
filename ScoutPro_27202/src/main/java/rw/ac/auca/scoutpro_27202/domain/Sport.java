package rw.ac.auca.scoutpro_27202.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "sports")
public class Sport extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;   // e.g. FOOTBALL, BASKETBALL

    public Sport() {}

    public Sport(String name, String code) {
        this.name = name;
        this.code = code;
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}