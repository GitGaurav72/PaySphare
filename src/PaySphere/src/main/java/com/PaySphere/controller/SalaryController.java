package com.PaySphere.controller;

import com.PaySphere.dto.salary.SalaryCreateRequest;
import com.PaySphere.dto.salary.SalaryResponse;
import com.PaySphere.security.AppUserPrincipal;
import com.PaySphere.service.SalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees/{employeeId}")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    @GetMapping("/salary")
    public ResponseEntity<SalaryResponse> getCurrentSalary(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryService.getCurrentSalary(employeeId));
    }

    @GetMapping("/salary-history")
    public ResponseEntity<List<SalaryResponse>> getSalaryHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryService.getSalaryHistory(employeeId));
    }

    @PostMapping("/salary")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<SalaryResponse> createSalaryChange(@PathVariable Long employeeId,
                                                               @Valid @RequestBody SalaryCreateRequest request,
                                                               @AuthenticationPrincipal AppUserPrincipal actor) {
        SalaryResponse created = salaryService.createSalaryChange(employeeId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
