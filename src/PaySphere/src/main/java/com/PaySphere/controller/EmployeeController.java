package com.PaySphere.controller;

import com.PaySphere.dto.common.PageResponse;
import com.PaySphere.dto.employee.EmployeeCreateRequest;
import com.PaySphere.dto.employee.EmployeeResponse;
import com.PaySphere.dto.employee.EmployeeStatusUpdateRequest;
import com.PaySphere.dto.employee.EmployeeSummaryResponse;
import com.PaySphere.dto.employee.EmployeeUpdateRequest;
import com.PaySphere.entity.EmployeeStatus;
import com.PaySphere.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeSummaryResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                employeeService.search(search, countryId, departmentId, designationId, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse created = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody EmployeeStatusUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateStatus(id, request));
    }
}
