package com.PaySphere.dto.employee;

import com.PaySphere.entity.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusUpdateRequest(

        @NotNull(message = "Status is required")
        EmployeeStatus status
) {
}
