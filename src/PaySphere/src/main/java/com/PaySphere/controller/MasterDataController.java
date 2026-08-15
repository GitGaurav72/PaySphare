package com.PaySphere.controller;

import com.PaySphere.dto.employee.CountryResponse;
import com.PaySphere.dto.employee.DepartmentResponse;
import com.PaySphere.dto.employee.DesignationResponse;
import com.PaySphere.repository.CountryRepository;
import com.PaySphere.repository.DepartmentRepository;
import com.PaySphere.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MasterDataController {

    private final CountryRepository countryRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    @GetMapping("/api/countries")
    public ResponseEntity<List<CountryResponse>> getCountries() {
        return ResponseEntity.ok(countryRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getName()))
                .map(c -> new CountryResponse(c.getId(), c.getName(), c.getCountryCode(), c.getCurrencyCode()))
                .toList());
    }

    @GetMapping("/api/departments")
    public ResponseEntity<List<DepartmentResponse>> getDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(d -> d.getName()))
                .map(d -> new DepartmentResponse(d.getId(), d.getName(), d.getDescription()))
                .toList());
    }

    @GetMapping("/api/designations")
    public ResponseEntity<List<DesignationResponse>> getDesignations() {
        return ResponseEntity.ok(designationRepository.findAll().stream()
                .sorted(Comparator.comparing(d -> d.getName()))
                .map(d -> new DesignationResponse(d.getId(), d.getName()))
                .toList());
    }
}
