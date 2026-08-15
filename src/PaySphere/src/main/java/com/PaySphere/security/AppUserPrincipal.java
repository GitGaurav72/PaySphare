package com.PaySphere.security;

import com.PaySphere.entity.HrRole;
import com.PaySphere.entity.HrUser;
import com.PaySphere.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final HrRole role;
    private final boolean enabled;

    public AppUserPrincipal(HrUser hrUser) {
        this.id = hrUser.getId();
        this.name = hrUser.getName();
        this.email = hrUser.getEmail();
        this.passwordHash = hrUser.getPasswordHash();
        this.role = hrUser.getRole();
        this.enabled = hrUser.getStatus() == UserStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public HrRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
