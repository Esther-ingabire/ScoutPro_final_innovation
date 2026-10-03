package rw.ac.auca.scoutpro_27202.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.scoutpro_27202.domain.Sport;

import java.util.Optional;
import java.util.UUID;

public interface SportRepository extends JpaRepository<Sport, UUID> {
    Optional<Sport> findByCode(String code);
    boolean existsByCode(String code);
}