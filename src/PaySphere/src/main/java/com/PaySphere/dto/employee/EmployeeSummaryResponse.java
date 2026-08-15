package com.PaySphere.dto.employee;

import com.PaySphere.entity.EmployeeStatus;

import java.time.LocalDate;

public record EmployeeSummaryResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String countryName,
        String departmentName,
        String designationName,
        LocalDate joiningDate,
        EmployeeStatus status
) {
}
