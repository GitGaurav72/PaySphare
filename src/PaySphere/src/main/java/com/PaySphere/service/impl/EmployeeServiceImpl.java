package com.PaySphere.service.impl;

import com.PaySphere.dto.employee.EmployeeCreateRequest;
import com.PaySphere.dto.employee.EmployeeResponse;
import com.PaySphere.dto.employee.EmployeeStatusUpdateRequest;
import com.PaySphere.dto.employee.EmployeeSummaryResponse;
import com.PaySphere.dto.employee.EmployeeUpdateRequest;
import com.PaySphere.entity.Country;
import com.PaySphere.entity.Department;
import com.PaySphere.entity.Designation;
import com.PaySphere.entity.Employee;
import com.PaySphere.entity.EmployeeStatus;
import com.PaySphere.exception.DuplicateResourceException;
import com.PaySphere.exception.ResourceNotFoundException;
import com.PaySphere.mapper.EmployeeMapper;
import com.PaySphere.repository.CountryRepository;
import com.PaySphere.repository.DepartmentRepository;
import com.PaySphere.repository.DesignationRepository;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.service.EmployeeService;
import com.PaySphere.specification.EmployeeSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String EMPLOYEE_CODE_FORMAT = "EMP%06d";

    private final EmployeeRepository employeeRepository;
    private final CountryRepository countryRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSummaryResponse> search(String search,
                                                 Long countryId,
                                                 Long departmentId,
                                                 Long designationId,
                                                 EmployeeStatus status,
                                                 Pageable pageable) {
        Page<Employee> employees = employeeRepository.findAll(
                EmployeeSpecifications.withFilters(search, countryId, departmentId, designationId, status),
                pageable);
        return employees.map(employeeMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An employee with email '" + request.email() + "' already exists");
        }

        Country country = findCountryOrThrow(request.countryId());
        Department department = findDepartmentOrThrow(request.departmentId());
        Designation designation = findDesignationOrThrow(request.designationId());

        Employee employee = Employee.builder()
                .employeeCode(generateNextEmployeeCode())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .country(country)
                .department(department)
                .designation(designation)
                .joiningDate(request.joiningDate())
                .status(EmployeeStatus.ACTIVE)
                .build();

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = findEmployeeOrThrow(id);

        if (!employee.getEmail().equalsIgnoreCase(request.email())
                && employeeRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An employee with email '" + request.email() + "' already exists");
        }

        Country country = findCountryOrThrow(request.countryId());
        Department department = findDepartmentOrThrow(request.departmentId());
        Designation designation = findDesignationOrThrow(request.designationId());

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setCountry(country);
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setJoiningDate(request.joiningDate());

        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse updateStatus(Long id, EmployeeStatusUpdateRequest request) {
        Employee employee = findEmployeeOrThrow(id);
        employee.setStatus(request.status());
        return employeeMapper.toResponse(employee);
    }

    private String generateNextEmployeeCode() {
        int nextSequence = employeeRepository.findMaxEmployeeCodeSequence().orElse(0) + 1;
        return EMPLOYEE_CODE_FORMAT.formatted(nextSequence);
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private Country findCountryOrThrow(Long id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    private Designation findDesignationOrThrow(Long id) {
        return designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
    }
}
