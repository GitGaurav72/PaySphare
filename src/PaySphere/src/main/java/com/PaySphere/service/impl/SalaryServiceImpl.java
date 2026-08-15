package com.PaySphere.service.impl;

import com.PaySphere.dto.salary.SalaryCreateRequest;
import com.PaySphere.dto.salary.SalaryResponse;
import com.PaySphere.entity.Employee;
import com.PaySphere.entity.HrUser;
import com.PaySphere.entity.SalaryHistory;
import com.PaySphere.exception.BadRequestException;
import com.PaySphere.exception.ResourceNotFoundException;
import com.PaySphere.mapper.SalaryMapper;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.repository.HrUserRepository;
import com.PaySphere.repository.SalaryHistoryRepository;
import com.PaySphere.security.AppUserPrincipal;
import com.PaySphere.service.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalaryServiceImpl implements SalaryService {

    private final SalaryHistoryRepository salaryHistoryRepository;
    private final EmployeeRepository employeeRepository;
    private final HrUserRepository hrUserRepository;
    private final SalaryMapper salaryMapper;

    @Override
    @Transactional(readOnly = true)
    public SalaryResponse getCurrentSalary(Long employeeId) {
        ensureEmployeeExists(employeeId);
        SalaryHistory current = salaryHistoryRepository.findCurrentByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No current salary found for employee id: " + employeeId));
        return salaryMapper.toResponse(current);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryResponse> getSalaryHistory(Long employeeId) {
        ensureEmployeeExists(employeeId);
        return salaryHistoryRepository.findAllByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(salaryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SalaryResponse createSalaryChange(Long employeeId, SalaryCreateRequest request, AppUserPrincipal actor) {
        // Locking the employee row serializes concurrent salary changes for this employee,
        // preventing two transactions from both seeing "no current salary" and each
        // inserting a current (effective_to IS NULL) record.
        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        HrUser createdBy = hrUserRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("HR user not found with id: " + actor.getId()));

        Optional<SalaryHistory> currentOpt = salaryHistoryRepository.findCurrentByEmployeeId(employeeId);

        if (currentOpt.isPresent()) {
            SalaryHistory current = currentOpt.get();
            if (!request.effectiveFrom().isAfter(current.getEffectiveFrom())) {
                throw new BadRequestException(
                        "New salary effective date must be after the current salary's effective date ("
                                + current.getEffectiveFrom() + ")");
            }
            current.setEffectiveTo(request.effectiveFrom().minusDays(1));
        }

        SalaryHistory newSalary = SalaryHistory.builder()
                .employee(employee)
                .currencyCode(request.currencyCode())
                .baseSalary(request.baseSalary())
                .bonus(request.bonus() != null ? request.bonus() : BigDecimal.ZERO)
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(null)
                .createdBy(createdBy)
                .build();

        SalaryHistory saved = salaryHistoryRepository.save(newSalary);
        return salaryMapper.toResponse(saved);
    }

    private void ensureEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
    }
}
