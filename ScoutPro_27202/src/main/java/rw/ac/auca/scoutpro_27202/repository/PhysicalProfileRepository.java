package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.PhysicalProfile;

import java.util.Optional;
import java.util.UUID;

public interface PhysicalProfileRepository extends JpaRepository<PhysicalProfile, UUID> {
    Optional<PhysicalProfile> findByAthleteId(UUID athleteId);
}