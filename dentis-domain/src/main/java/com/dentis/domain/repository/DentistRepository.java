package com.dentis.domain.repository;

import com.dentis.domain.entity.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DentistRepository extends JpaRepository<Dentist, String> {

    Optional<Dentist> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Dentist> findByActiveTrue();
}
