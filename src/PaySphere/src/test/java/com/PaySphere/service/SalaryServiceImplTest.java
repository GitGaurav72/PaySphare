package com.PaySphere.service;

import com.PaySphere.dto.salary.SalaryCreateRequest;
import com.PaySphere.dto.salary.SalaryResponse;
import com.PaySphere.entity.Country;
import com.PaySphere.entity.Department;
import com.PaySphere.entity.Designation;
import com.PaySphere.entity.Employee;
import com.PaySphere.entity.HrRole;
import com.PaySphere.entity.HrUser;
import com.PaySphere.entity.PaymentStatus;
import com.PaySphere.entity.SalaryHistory;
import com.PaySphere.entity.UserStatus;
import com.PaySphere.exception.BadRequestException;
import com.PaySphere.exception.ResourceNotFoundException;
import com.PaySphere.mapper.SalaryMapper;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.repository.HrUserRepository;
import com.PaySphere.repository.SalaryHistoryRepository;
import com.PaySphere.security.AppUserPrincipal;
import com.PaySphere.service.impl.SalaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryServiceImplTest {

    @Mock
    private SalaryHistoryRepository salaryHistoryRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private HrUserRepository hrUserRepository;
    @Mock
    private ExcelReportService excelReportService;
    @Mock
    private EmailService emailService;

    private SalaryServiceImpl salaryService;

    private Employee employee;
    private HrUser hrUser;
    private AppUserPrincipal actor;

    @BeforeEach
    void setUp() {
        salaryService = new SalaryServiceImpl(
                salaryHistoryRepository, employeeRepository, hrUserRepository, new SalaryMapper(),
                excelReportService, emailService);

        employee = Employee.builder()
                .id(10L)
                .employeeCode("EMP000010")
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .country(Country.builder().id(1L).name("India").countryCode("IN").currencyCode("INR").build())
                .department(Department.builder().id(1L).name("Engineering").build())
                .designation(Designation.builder().id(1L).name("Software Engineer").build())
                .build();
        hrUser = HrUser.builder().id(5L).name("HR Manager").role(HrRole.HR_MANAGER).status(UserStatus.ACTIVE)
                .email("manager@paysphere.com").passwordHash("hash").build();
        actor = new AppUserPrincipal(hrUser);
    }

    @Test
    void createSalaryChange_withNoExistingSalary_insertsFirstCurrentRecord() {
        when(employeeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(employee));
        when(hrUserRepository.findById(5L)).thenReturn(Optional.of(hrUser));
        when(salaryHistoryRepository.findCurrentByEmployeeId(10L)).thenReturn(Optional.empty());
        when(salaryHistoryRepository.save(any(SalaryHistory.class))).thenAnswer(inv -> {
            SalaryHistory s = inv.getArgument(0);
            s.setId(1L);
            s.setCreatedBy(hrUser);
            s.setEmployee(employee);
            return s;
        });

        SalaryCreateRequest request = new SalaryCreateRequest("INR", new BigDecimal("1000000"), new BigDecimal("50000"), LocalDate.of(2026, 1, 1));

        SalaryResponse response = salaryService.createSalaryChange(10L, request, actor);

        assertThat(response.effectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.effectiveTo()).isNull();
        verify(salaryHistoryRepository).save(any(SalaryHistory.class));
    }

    @Test
    void createSalaryChange_withExistingCurrentSalary_closesOldRecordAndInsertsNew() {
        SalaryHistory current = SalaryHistory.builder()
                .id(1L)
                .employee(employee)
                .currencyCode("INR")
                .baseSalary(new BigDecimal("1000000"))
                .bonus(new BigDecimal("50000"))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .effectiveTo(null)
                .createdBy(hrUser)
                .build();

        when(employeeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(employee));
        when(hrUserRepository.findById(5L)).thenReturn(Optional.of(hrUser));
        when(salaryHistoryRepository.findCurrentByEmployeeId(10L)).thenReturn(Optional.of(current));
        when(salaryHistoryRepository.save(any(SalaryHistory.class))).thenAnswer(inv -> {
            SalaryHistory s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        SalaryCreateRequest request = new SalaryCreateRequest("INR", new BigDecimal("1200000"), new BigDecimal("60000"), LocalDate.of(2026, 8, 1));

        SalaryResponse response = salaryService.createSalaryChange(10L, request, actor);

        // Old record must be closed the day before the new record starts.
        assertThat(current.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 7, 31));
        // New record is the current one (effectiveTo == null).
        assertThat(response.effectiveFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.effectiveTo()).isNull();
        assertThat(response.baseSalary()).isEqualByComparingTo("1200000");

        ArgumentCaptor<SalaryHistory> captor = ArgumentCaptor.forClass(SalaryHistory.class);
        verify(salaryHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getEffectiveTo()).isNull();
    }

    @Test
    void createSalaryChange_withEffectiveDateNotAfterCurrent_throwsBadRequest() {
        SalaryHistory current = SalaryHistory.builder()
                .id(1L)
                .employee(employee)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .effectiveTo(null)
                .build();

        when(employeeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(employee));
        when(hrUserRepository.findById(5L)).thenReturn(Optional.of(hrUser));
        when(salaryHistoryRepository.findCurrentByEmployeeId(10L)).thenReturn(Optional.of(current));

        SalaryCreateRequest sameDate = new SalaryCreateRequest("INR", new BigDecimal("1200000"), null, LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> salaryService.createSalaryChange(10L, sameDate, actor))
                .isInstanceOf(BadRequestException.class);

        verify(salaryHistoryRepository, never()).save(any());
    }

    @Test
    void createSalaryChange_withUnknownEmployee_throwsResourceNotFoundException() {
        when(employeeRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        SalaryCreateRequest request = new SalaryCreateRequest("INR", new BigDecimal("1000000"), null, LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> salaryService.createSalaryChange(404L, request, actor))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsPaid_withPendingSalary_marksPaidAndEmailsPayslip() {
        SalaryHistory salary = SalaryHistory.builder()
                .id(1L)
                .employee(employee)
                .currencyCode("INR")
                .baseSalary(new BigDecimal("1000000"))
                .bonus(new BigDecimal("50000"))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .createdBy(hrUser)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(salaryHistoryRepository.findById(1L)).thenReturn(Optional.of(salary));
        when(excelReportService.generatePayslip(salary)).thenReturn(new byte[]{1, 2, 3});

        SalaryResponse response = salaryService.markAsPaid(10L, 1L);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
        verify(emailService).sendPayslip(
                org.mockito.ArgumentMatchers.eq("jane.doe@example.com"),
                org.mockito.ArgumentMatchers.eq("Jane Doe"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void markAsPaid_whenAlreadyPaid_throwsBadRequest() {
        SalaryHistory salary = SalaryHistory.builder()
                .id(1L)
                .employee(employee)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(salaryHistoryRepository.findById(1L)).thenReturn(Optional.of(salary));

        assertThatThrownBy(() -> salaryService.markAsPaid(10L, 1L))
                .isInstanceOf(BadRequestException.class);

        verify(emailService, never()).sendPayslip(any(), any(), any(), any());
    }

    @Test
    void markAsPaid_withSalaryBelongingToDifferentEmployee_throwsResourceNotFoundException() {
        SalaryHistory salary = SalaryHistory.builder()
                .id(1L)
                .employee(employee)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(salaryHistoryRepository.findById(1L)).thenReturn(Optional.of(salary));

        assertThatThrownBy(() -> salaryService.markAsPaid(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
