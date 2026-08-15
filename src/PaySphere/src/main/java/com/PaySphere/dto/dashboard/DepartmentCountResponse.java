package com.PaySphere.dto.dashboard;

public record DepartmentCountResponse(
        String departmentName,
        long employeeCount
) {
}
