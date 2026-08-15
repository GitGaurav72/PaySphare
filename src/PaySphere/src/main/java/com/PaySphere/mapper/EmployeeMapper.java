package com.PaySphere.mapper;

import com.PaySphere.dto.employee.EmployeeResponse;
import com.PaySphere.dto.employee.EmployeeSummaryResponse;
import com.PaySphere.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getCountry().getId(),
                employee.getCountry().getName(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getDesignation().getId(),
                employee.getDesignation().getName(),
                employee.getJoiningDate(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    public EmployeeSummaryResponse toSummaryResponse(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getCountry().getName(),
                employee.getDepartment().getName(),
                employee.getDesignation().getName(),
                employee.getJoiningDate(),
                employee.getStatus()
        );
    }
}
