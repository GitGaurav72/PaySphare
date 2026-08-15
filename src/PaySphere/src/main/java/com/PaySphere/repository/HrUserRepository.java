package com.PaySphere.repository;

import com.PaySphere.entity.HrUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrUserRepository extends JpaRepository<HrUser, Long> {

    Optional<HrUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
