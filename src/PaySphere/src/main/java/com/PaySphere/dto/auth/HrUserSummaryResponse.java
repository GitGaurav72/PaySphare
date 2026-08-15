package com.PaySphere.dto.auth;

import com.PaySphere.entity.HrRole;

public record HrUserSummaryResponse(
        Long id,
        String name,
        String email,
        HrRole role
) {
}
