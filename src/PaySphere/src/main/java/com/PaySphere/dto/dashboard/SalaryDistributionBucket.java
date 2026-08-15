package com.PaySphere.dto.dashboard;

import java.math.BigDecimal;

public record SalaryDistributionBucket(
        BigDecimal rangeStart,
        BigDecimal rangeEnd,
        long employeeCount
) {
}
