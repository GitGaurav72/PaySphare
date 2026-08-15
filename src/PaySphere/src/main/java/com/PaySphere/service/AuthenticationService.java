package com.PaySphere.service;

import com.PaySphere.dto.auth.LoginRequest;
import com.PaySphere.dto.auth.LoginResponse;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request);
}
