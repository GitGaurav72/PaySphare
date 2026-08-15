package com.PaySphere.dto.employee;

import com.PaySphere.entity.EmployeeStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        Long countryId,
        String countryName,
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        LocalDate joiningDate,
        EmployeeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
