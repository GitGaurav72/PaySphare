package com.PaySphere.dto.dashboard;

import java.util.List;

public record SalaryDistributionResponse(
        String currencyCode,
        List<SalaryDistributionBucket> buckets
) {
}
