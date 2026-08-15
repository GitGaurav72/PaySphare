package com.PaySphere.controller;

import com.PaySphere.dto.hruser.HrUserCreateRequest;
import com.PaySphere.dto.hruser.HrUserResponse;
import com.PaySphere.dto.hruser.HrUserUpdateRequest;
import com.PaySphere.service.HrUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr-users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR_ADMIN')")
public class HrUserController {

    private final HrUserService hrUserService;

    @GetMapping
    public ResponseEntity<List<HrUserResponse>> getAll() {
        return ResponseEntity.ok(hrUserService.getAll());
    }

    @PostMapping
    public ResponseEntity<HrUserResponse> create(@Valid @RequestBody HrUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hrUserService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HrUserResponse> update(@PathVariable Long id, @Valid @RequestBody HrUserUpdateRequest request) {
        return ResponseEntity.ok(hrUserService.update(id, request));
    }
}
