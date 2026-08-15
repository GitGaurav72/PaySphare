package com.PaySphere.dto.hruser;

import com.PaySphere.entity.HrRole;
import com.PaySphere.entity.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HrUserUpdateRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotNull(message = "Role is required")
        HrRole role,

        @NotNull(message = "Status is required")
        UserStatus status
) {
}
