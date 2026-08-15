package com.PaySphere.service;

import com.PaySphere.dto.employee.EmployeeCreateRequest;
import com.PaySphere.dto.employee.EmployeeResponse;
import com.PaySphere.dto.employee.EmployeeStatusUpdateRequest;
import com.PaySphere.dto.employee.EmployeeSummaryResponse;
import com.PaySphere.dto.employee.EmployeeUpdateRequest;
import com.PaySphere.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    Page<EmployeeSummaryResponse> search(String search,
                                          Long countryId,
                                          Long departmentId,
                                          Long designationId,
                                          EmployeeStatus status,
                                          Pageable pageable);

    EmployeeResponse getById(Long id);

    EmployeeResponse create(EmployeeCreateRequest request);

    EmployeeResponse update(Long id, EmployeeUpdateRequest request);

    EmployeeResponse updateStatus(Long id, EmployeeStatusUpdateRequest request);
}
