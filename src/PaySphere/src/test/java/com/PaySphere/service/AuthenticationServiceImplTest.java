package com.PaySphere.service;

import com.PaySphere.dto.auth.LoginRequest;
import com.PaySphere.dto.auth.LoginResponse;
import com.PaySphere.entity.HrRole;
import com.PaySphere.entity.HrUser;
import com.PaySphere.entity.UserStatus;
import com.PaySphere.security.AppUserPrincipal;
import com.PaySphere.security.JwtService;
import com.PaySphere.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(authenticationManager, jwtService);
    }

    @Test
    void login_withValidCredentials_returnsTokenAndUserSummary() {
        HrUser hrUser = HrUser.builder()
                .id(1L)
                .name("System Admin")
                .email("admin@paysphere.com")
                .passwordHash("hashed")
                .role(HrRole.HR_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        AppUserPrincipal principal = new AppUserPrincipal(hrUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtService.generateToken(1L, "admin@paysphere.com", "HR_ADMIN")).thenReturn("signed-jwt");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        LoginResponse response = authenticationService.login(new LoginRequest("admin@paysphere.com", "password"));

        assertThat(response.accessToken()).isEqualTo("signed-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user().email()).isEqualTo("admin@paysphere.com");
        assertThat(response.user().role()).isEqualTo(HrRole.HR_ADMIN);
    }

    @Test
    void login_withInvalidCredentials_throwsBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("admin@paysphere.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
