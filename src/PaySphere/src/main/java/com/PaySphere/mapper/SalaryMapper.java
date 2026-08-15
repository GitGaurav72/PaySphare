package com.PaySphere.mapper;

import com.PaySphere.dto.salary.SalaryResponse;
import com.PaySphere.entity.SalaryHistory;
import org.springframework.stereotype.Component;

@Component
public class SalaryMapper {

    public SalaryResponse toResponse(SalaryHistory salaryHistory) {
        return new SalaryResponse(
                salaryHistory.getId(),
                salaryHistory.getEmployee().getId(),
                salaryHistory.getCurrencyCode(),
                salaryHistory.getBaseSalary(),
                salaryHistory.getBonus(),
                salaryHistory.getEffectiveFrom(),
                salaryHistory.getEffectiveTo(),
                salaryHistory.getCreatedBy().getId(),
                salaryHistory.getCreatedBy().getName(),
                salaryHistory.getCreatedAt()
        );
    }
}
