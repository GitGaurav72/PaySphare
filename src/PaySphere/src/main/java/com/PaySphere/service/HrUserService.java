package com.PaySphere.service;

import com.PaySphere.dto.hruser.HrUserCreateRequest;
import com.PaySphere.dto.hruser.HrUserResponse;
import com.PaySphere.dto.hruser.HrUserUpdateRequest;

import java.util.List;

public interface HrUserService {

    List<HrUserResponse> getAll();

    HrUserResponse create(HrUserCreateRequest request);

    HrUserResponse update(Long id, HrUserUpdateRequest request);
}
