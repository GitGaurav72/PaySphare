package com.PaySphere.repository;

import com.PaySphere.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {

    Optional<Designation> findByNameIgnoreCase(String name);
}
