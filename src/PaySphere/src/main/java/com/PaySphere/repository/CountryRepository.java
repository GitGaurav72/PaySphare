package com.PaySphere.repository;

import com.PaySphere.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByNameIgnoreCase(String name);
}
