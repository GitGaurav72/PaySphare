package com.PaySphere.service;

import com.PaySphere.dto.employee.EmployeeCreateRequest;
import com.PaySphere.dto.employee.EmployeeResponse;
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
import com.PaySphere.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DesignationRepository designationRepository;

    private EmployeeServiceImpl employeeService;

    private Country country;
    private Department department;
    private Designation designation;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(
                employeeRepository, countryRepository, departmentRepository, designationRepository, new EmployeeMapper());

        country = Country.builder().id(1L).name("India").countryCode("IN").currencyCode("INR").build();
        department = Department.builder().id(2L).name("Engineering").build();
        designation = Designation.builder().id(3L).name("Software Engineer").build();
    }

    @Test
    void create_withNewEmail_generatesSequentialEmployeeCodeAndSaves() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "John", "Smith", "john.smith@example.com", 1L, 2L, 3L, LocalDate.of(2024, 1, 15));

        when(employeeRepository.existsByEmailIgnoreCase("john.smith@example.com")).thenReturn(false);
        when(countryRepository.findById(1L)).thenReturn(Optional.of(country));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(designationRepository.findById(3L)).thenReturn(Optional.of(designation));
        when(employeeRepository.findMaxEmployeeCodeSequence()).thenReturn(Optional.of(41));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        EmployeeResponse response = employeeService.create(request);

        assertThat(response.employeeCode()).isEqualTo("EMP000042");
        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.countryName()).isEqualTo("India");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void create_withDuplicateEmail_throwsDuplicateResourceException() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "John", "Smith", "existing@example.com", 1L, 2L, 3L, LocalDate.of(2024, 1, 15));

        when(employeeRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    void create_withUnknownCountry_throwsResourceNotFoundException() {
        EmployeeCreateRequest request = new EmployeeCreateRequest(
                "John", "Smith", "john.smith@example.com", 99L, 2L, 3L, LocalDate.of(2024, 1, 15));

        when(employeeRepository.existsByEmailIgnoreCase("john.smith@example.com")).thenReturn(false);
        when(countryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country");
    }

    @Test
    void getById_withUnknownId_throwsResourceNotFoundException() {
        when(employeeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
