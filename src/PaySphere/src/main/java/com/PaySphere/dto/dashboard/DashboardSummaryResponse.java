package com.PaySphere.dto.dashboard;

import java.math.BigDecimal;

/**
 * Compensation figures are scoped to {@code primaryCurrencyCode} (the currency with the
 * largest current headcount) rather than blended across currencies, since salaries in
 * different currencies are not directly comparable without FX conversion. For a full
 * per-currency breakdown, see /api/dashboard/salary-by-country.
 */
public record DashboardSummaryResponse(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long onLeaveEmployees,
        long terminatedEmployees,
        String primaryCurrencyCode,
        BigDecimal averageTotalCompensation,
        BigDecimal highestTotalCompensation,
        BigDecimal lowestTotalCompensation
) {
}
