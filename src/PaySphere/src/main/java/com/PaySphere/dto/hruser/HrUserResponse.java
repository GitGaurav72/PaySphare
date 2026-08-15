package com.PaySphere.dto.hruser;

import com.PaySphere.entity.HrRole;
import com.PaySphere.entity.UserStatus;

import java.time.LocalDateTime;

public record HrUserResponse(
        Long id,
        String name,
        String email,
        HrRole role,
        UserStatus status,
        LocalDateTime createdAt
) {
}
