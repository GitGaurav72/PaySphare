package com.PaySphere.service;

import com.PaySphere.dto.salary.SalaryCreateRequest;
import com.PaySphere.dto.salary.SalaryResponse;
import com.PaySphere.security.AppUserPrincipal;

import java.util.List;

public interface SalaryService {

    SalaryResponse getCurrentSalary(Long employeeId);

    List<SalaryResponse> getSalaryHistory(Long employeeId);

    SalaryResponse createSalaryChange(Long employeeId, SalaryCreateRequest request, AppUserPrincipal actor);

    SalaryResponse markAsPaid(Long employeeId, Long salaryId);
}
