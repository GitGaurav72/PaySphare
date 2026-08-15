package com.PaySphere.service.impl;

import com.PaySphere.dto.auth.HrUserSummaryResponse;
import com.PaySphere.dto.auth.LoginRequest;
import com.PaySphere.dto.auth.LoginResponse;
import com.PaySphere.security.AppUserPrincipal;
import com.PaySphere.security.JwtService;
import com.PaySphere.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        AppUserPrincipal principal;
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            principal = (AppUserPrincipal) authentication.getPrincipal();
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for email: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(principal.getId(), principal.getUsername(), principal.getRole().name());

        HrUserSummaryResponse userSummary = new HrUserSummaryResponse(
                principal.getId(), principal.getName(), principal.getUsername(), principal.getRole());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs() / 1000, userSummary);
    }
}
