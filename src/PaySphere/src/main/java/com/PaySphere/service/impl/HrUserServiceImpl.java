package com.PaySphere.service.impl;

import com.PaySphere.dto.hruser.HrUserCreateRequest;
import com.PaySphere.dto.hruser.HrUserResponse;
import com.PaySphere.dto.hruser.HrUserUpdateRequest;
import com.PaySphere.entity.HrUser;
import com.PaySphere.entity.UserStatus;
import com.PaySphere.exception.DuplicateResourceException;
import com.PaySphere.exception.ResourceNotFoundException;
import com.PaySphere.repository.HrUserRepository;
import com.PaySphere.service.HrUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrUserServiceImpl implements HrUserService {

    private final HrUserRepository hrUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<HrUserResponse> getAll() {
        return hrUserRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public HrUserResponse create(HrUserCreateRequest request) {
        if (hrUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An HR user with email '" + request.email() + "' already exists");
        }

        HrUser hrUser = HrUser.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        return toResponse(hrUserRepository.save(hrUser));
    }

    @Override
    @Transactional
    public HrUserResponse update(Long id, HrUserUpdateRequest request) {
        HrUser hrUser = hrUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HR user not found with id: " + id));

        hrUser.setName(request.name());
        hrUser.setRole(request.role());
        hrUser.setStatus(request.status());

        return toResponse(hrUser);
    }

    private HrUserResponse toResponse(HrUser hrUser) {
        return new HrUserResponse(
                hrUser.getId(), hrUser.getName(), hrUser.getEmail(),
                hrUser.getRole(), hrUser.getStatus(), hrUser.getCreatedAt());
    }
}
