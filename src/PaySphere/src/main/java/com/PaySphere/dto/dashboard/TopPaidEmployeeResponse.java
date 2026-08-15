package com.PaySphere.dto.dashboard;

import java.math.BigDecimal;

public record TopPaidEmployeeResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        String departmentName,
        String countryName,
        String currencyCode,
        BigDecimal totalCompensation
) {
}
