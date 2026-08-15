package com.PaySphere.dto.dashboard;

import java.math.BigDecimal;

public record CountrySalaryResponse(
        String countryName,
        String currencyCode,
        BigDecimal averageTotalCompensation,
        BigDecimal minTotalCompensation,
        BigDecimal maxTotalCompensation,
        long employeeCount
) {
}
