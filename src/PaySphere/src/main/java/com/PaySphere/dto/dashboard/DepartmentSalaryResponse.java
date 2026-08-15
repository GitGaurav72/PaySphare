package com.PaySphere.dto.dashboard;

import java.math.BigDecimal;

public record DepartmentSalaryResponse(
        String departmentName,
        String currencyCode,
        BigDecimal averageTotalCompensation,
        long employeeCount
) {
}
