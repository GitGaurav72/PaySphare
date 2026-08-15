package com.PaySphere.service;

import com.PaySphere.dto.employee.BulkUploadResponse;
import com.PaySphere.entity.Country;
import com.PaySphere.entity.Department;
import com.PaySphere.entity.Designation;
import com.PaySphere.entity.Employee;
import com.PaySphere.repository.CountryRepository;
import com.PaySphere.repository.DepartmentRepository;
import com.PaySphere.repository.DesignationRepository;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.service.impl.EmployeeBulkImportServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeBulkImportServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DesignationRepository designationRepository;

    private EmployeeBulkImportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeBulkImportServiceImpl(employeeRepository, countryRepository, departmentRepository, designationRepository);

        Country india = Country.builder().id(1L).name("India").countryCode("IN").currencyCode("INR").build();
        Department engineering = Department.builder().id(1L).name("Engineering").build();
        Designation swe = Designation.builder().id(1L).name("Software Engineer").build();

        when(countryRepository.findAll()).thenReturn(List.of(india));
        when(departmentRepository.findAll()).thenReturn(List.of(engineering));
        when(designationRepository.findAll()).thenReturn(List.of(swe));
        when(employeeRepository.findMaxEmployeeCodeSequence()).thenReturn(Optional.of(5));
    }

    private MockMultipartFile buildWorkbook(String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");
            sheet.createRow(0); // header row, content irrelevant to the parser
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int col = 0; col < rows[i].length; col++) {
                    if (rows[i][col] != null) {
                        row.createCell(col).setCellValue(rows[i][col]);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile("file", "upload.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void importEmployees_withValidRow_createsEmployee() throws Exception {
        when(employeeRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(employeeRepository.save(org.mockito.ArgumentMatchers.any(Employee.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildWorkbook(new String[][]{
                {"John", "Smith", "john.smith@example.com", "India", "Engineering", "Software Engineer", "2024-01-15"}
        });

        BulkUploadResponse response = service.importEmployees(file);

        assertThat(response.totalRows()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failureCount()).isEqualTo(0);
        assertThat(response.results().get(0).employeeCode()).isEqualTo("EMP000006");
    }

    @Test
    void importEmployees_withUnknownCountry_reportsRowFailureWithoutThrowing() throws Exception {
        MockMultipartFile file = buildWorkbook(new String[][]{
                {"John", "Smith", "john.smith@example.com", "Atlantis", "Engineering", "Software Engineer", "2024-01-15"}
        });

        BulkUploadResponse response = service.importEmployees(file);

        assertThat(response.successCount()).isEqualTo(0);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.results().get(0).message()).contains("Unknown country");
    }

    @Test
    void importEmployees_withDuplicateEmailWithinFile_reportsSecondRowAsFailure() throws Exception {
        when(employeeRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(employeeRepository.save(org.mockito.ArgumentMatchers.any(Employee.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildWorkbook(new String[][]{
                {"John", "Smith", "dup@example.com", "India", "Engineering", "Software Engineer", "2024-01-15"},
                {"Jane", "Doe", "dup@example.com", "India", "Engineering", "Software Engineer", "2024-02-01"}
        });

        BulkUploadResponse response = service.importEmployees(file);

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.results().get(1).message()).contains("Duplicate email");
    }

    @Test
    void importEmployees_withInvalidDate_reportsRowFailure() throws Exception {
        MockMultipartFile file = buildWorkbook(new String[][]{
                {"John", "Smith", "john.smith@example.com", "India", "Engineering", "Software Engineer", "not-a-date"}
        });

        BulkUploadResponse response = service.importEmployees(file);

        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.results().get(0).message()).contains("Joining date is not a valid date");
    }

    @Test
    void importEmployees_skipsBlankRows() throws Exception {
        when(employeeRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(employeeRepository.save(org.mockito.ArgumentMatchers.any(Employee.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildWorkbook(new String[][]{
                {"John", "Smith", "john.smith@example.com", "India", "Engineering", "Software Engineer", "2024-01-15"},
                {null, null, null, null, null, null, null},
                {"Jane", "Doe", "jane.doe@example.com", "India", "Engineering", "Software Engineer", "2024-02-01"}
        });

        BulkUploadResponse response = service.importEmployees(file);

        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(2);
    }
}
