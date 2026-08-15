package com.PaySphere.support;

import com.PaySphere.entity.Country;
import com.PaySphere.entity.Department;
import com.PaySphere.entity.Designation;
import com.PaySphere.entity.Employee;
import com.PaySphere.entity.EmployeeStatus;
import com.PaySphere.entity.HrRole;
import com.PaySphere.entity.HrUser;
import com.PaySphere.entity.UserStatus;
import com.PaySphere.repository.CountryRepository;
import com.PaySphere.repository.DepartmentRepository;
import com.PaySphere.repository.DesignationRepository;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.repository.HrUserRepository;
import com.PaySphere.repository.SalaryHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base class for API-level tests. Spins up the full Spring context against the H2 test
 * profile, seeds minimal master data + one HR user per role, and logs each of them in
 * through the real /api/auth/login endpoint so tests exercise the actual JWT issuance and
 * validation path rather than mocking security.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    protected static final String DEMO_PASSWORD = "Password@123";

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected HrUserRepository hrUserRepository;
    @Autowired
    protected CountryRepository countryRepository;
    @Autowired
    protected DepartmentRepository departmentRepository;
    @Autowired
    protected DesignationRepository designationRepository;
    @Autowired
    protected EmployeeRepository employeeRepository;
    @Autowired
    protected SalaryHistoryRepository salaryHistoryRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected MockMvc mockMvc;

    protected Country country;
    protected Department department;
    protected Designation designation;

    protected String adminToken;
    protected String managerToken;
    protected String viewerToken;

    @BeforeEach
    void baseSetUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        salaryHistoryRepository.deleteAll();
        employeeRepository.deleteAll();
        hrUserRepository.deleteAll();
        designationRepository.deleteAll();
        departmentRepository.deleteAll();
        countryRepository.deleteAll();

        country = countryRepository.save(Country.builder().name("India").countryCode("IN").currencyCode("INR").build());
        department = departmentRepository.save(Department.builder().name("Engineering").description("Tech").build());
        designation = designationRepository.save(Designation.builder().name("Software Engineer").build());

        hrUserRepository.save(hrUser("System Admin", "admin@paysphere.com", HrRole.HR_ADMIN));
        hrUserRepository.save(hrUser("HR Manager", "manager@paysphere.com", HrRole.HR_MANAGER));
        hrUserRepository.save(hrUser("HR Viewer", "viewer@paysphere.com", HrRole.HR_VIEWER));

        adminToken = login("admin@paysphere.com");
        managerToken = login("manager@paysphere.com");
        viewerToken = login("viewer@paysphere.com");
    }

    private HrUser hrUser(String name, String email, HrRole role) {
        return HrUser.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    protected String login(String email) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, DEMO_PASSWORD);

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    protected Employee createEmployee(String employeeCode, String email) {
        return employeeRepository.save(Employee.builder()
                .employeeCode(employeeCode)
                .firstName("Test")
                .lastName("Employee")
                .email(email)
                .country(country)
                .department(department)
                .designation(designation)
                .joiningDate(LocalDate.of(2023, 6, 1))
                .status(EmployeeStatus.ACTIVE)
                .build());
    }
}
