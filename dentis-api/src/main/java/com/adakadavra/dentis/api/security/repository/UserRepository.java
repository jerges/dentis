package com.adakadavra.dentis.api.security.repository;

import com.adakadavra.dentis.api.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    List<User> findAllByClinicId(UUID clinicId);

    Optional<User> findByIdAndClinicId(UUID id, UUID clinicId);
}
