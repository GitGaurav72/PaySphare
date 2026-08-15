package com.PaySphere.dto.employee;

public record BulkUploadRowResult(
        int rowNumber,
        String email,
        boolean success,
        String employeeCode,
        String message
) {
}
