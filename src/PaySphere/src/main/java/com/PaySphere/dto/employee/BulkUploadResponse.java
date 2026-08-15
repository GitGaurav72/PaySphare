package com.PaySphere.dto.employee;

import java.util.List;

public record BulkUploadResponse(
        int totalRows,
        int successCount,
        int failureCount,
        List<BulkUploadRowResult> results
) {
}
