package com.PaySphere.dto.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SalaryResponse(
        Long id,
        Long employeeId,
        String currencyCode,
        BigDecimal baseSalary,
        BigDecimal bonus,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt
) {
}
