package com.PaySphere.seed;

import com.PaySphere.entity.HrRole;
import com.PaySphere.entity.HrUser;
import com.PaySphere.entity.UserStatus;
import com.PaySphere.repository.HrUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates realistic demo data for ~10,000 employees on a fresh database. Only runs when
 * app.seed.enabled=true (see application.yml / SEED_ENABLED env var) and is a no-op if the
 * employees table already has rows, so it is safe to leave the flag on across restarts.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int BATCH_SIZE = 500;
    private static final String DEMO_PASSWORD = "Password@123";

    private final JdbcTemplate jdbcTemplate;
    private final HrUserRepository hrUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Ishaan", "Rohan", "Kabir", "Arjun", "Sai", "Reyansh", "Krishna",
            "Ananya", "Diya", "Priya", "Saanvi", "Aadhya", "Meera", "Kavya", "Anika", "Riya", "Ishita",
            "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles",
            "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen",
            "Liam", "Noah", "Oliver", "Elijah", "Lucas", "Mason", "Ethan", "Logan", "Alexander", "Henry",
            "Emma", "Olivia", "Ava", "Sophia", "Isabella", "Mia", "Charlotte", "Amelia", "Harper", "Evelyn",
            "Mohammed", "Ahmed", "Omar", "Ali", "Hassan", "Fatima", "Aisha", "Layla", "Zainab", "Noor",
            "Lars", "Hans", "Klaus", "Stefan", "Anna", "Greta", "Ingrid", "Hannah", "Felix", "Max"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Gupta", "Patel", "Kumar", "Singh", "Reddy", "Rao", "Nair", "Iyer",
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia", "Wilson", "Anderson",
            "Taylor", "Thomas", "Moore", "Jackson", "Martin", "Lee", "Walker", "Hall", "Allen", "Young",
            "Khan", "Ahmed", "Ali", "Hussain", "Malik", "Chowdhury", "Rahman",
            "Muller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner",
            "Tan", "Lim", "Wong", "Lee", "Ng", "Goh",
            "Clarke", "Robinson", "Wright", "Evans", "Green", "King", "Baker"
    };

    private record CountrySalaryRange(BigDecimal min, BigDecimal max, BigDecimal roundTo) {
    }

    private static final Map<String, CountrySalaryRange> COUNTRY_SALARY_RANGES = Map.of(
            "IN", new CountrySalaryRange(new BigDecimal("400000"), new BigDecimal("4000000"), new BigDecimal("1000")),
            "US", new CountrySalaryRange(new BigDecimal("50000"), new BigDecimal("250000"), new BigDecimal("500")),
            "GB", new CountrySalaryRange(new BigDecimal("30000"), new BigDecimal("180000"), new BigDecimal("500")),
            "DE", new CountrySalaryRange(new BigDecimal("35000"), new BigDecimal("150000"), new BigDecimal("500")),
            "SG", new CountrySalaryRange(new BigDecimal("40000"), new BigDecimal("220000"), new BigDecimal("500")),
            "AU", new CountrySalaryRange(new BigDecimal("55000"), new BigDecimal("220000"), new BigDecimal("500")),
            "CA", new CountrySalaryRange(new BigDecimal("45000"), new BigDecimal("200000"), new BigDecimal("500")),
            "AE", new CountrySalaryRange(new BigDecimal("90000"), new BigDecimal("600000"), new BigDecimal("1000"))
    );

    private static final Map<String, Double> DESIGNATION_MULTIPLIER = new HashMap<>();

    static {
        DESIGNATION_MULTIPLIER.put("Software Engineer", 1.0);
        DESIGNATION_MULTIPLIER.put("Senior Software Engineer", 1.3);
        DESIGNATION_MULTIPLIER.put("Lead Software Engineer", 1.7);
        DESIGNATION_MULTIPLIER.put("Engineering Manager", 2.2);
        DESIGNATION_MULTIPLIER.put("Product Manager", 1.8);
        DESIGNATION_MULTIPLIER.put("Senior Product Manager", 2.3);
        DESIGNATION_MULTIPLIER.put("HR Manager", 1.6);
        DESIGNATION_MULTIPLIER.put("HR Executive", 1.0);
        DESIGNATION_MULTIPLIER.put("Financial Analyst", 1.1);
        DESIGNATION_MULTIPLIER.put("Finance Manager", 1.8);
        DESIGNATION_MULTIPLIER.put("Sales Executive", 0.9);
        DESIGNATION_MULTIPLIER.put("Sales Manager", 1.6);
        DESIGNATION_MULTIPLIER.put("Marketing Executive", 0.9);
        DESIGNATION_MULTIPLIER.put("Marketing Manager", 1.6);
        DESIGNATION_MULTIPLIER.put("Operations Manager", 1.7);
        DESIGNATION_MULTIPLIER.put("IT Administrator", 1.0);
        DESIGNATION_MULTIPLIER.put("Business Analyst", 1.1);
        DESIGNATION_MULTIPLIER.put("Legal Counsel", 1.9);
        DESIGNATION_MULTIPLIER.put("Customer Support Executive", 0.8);
        DESIGNATION_MULTIPLIER.put("Director", 2.8);
    }

    private static final EmployeeStatusWeight[] STATUS_WEIGHTS = {
            new EmployeeStatusWeight("ACTIVE", 0.85),
            new EmployeeStatusWeight("INACTIVE", 0.05),
            new EmployeeStatusWeight("ON_LEAVE", 0.05),
            new EmployeeStatusWeight("TERMINATED", 0.05)
    };

    private record EmployeeStatusWeight(String status, double weight) {
    }

    private record CountryRow(Long id, String code, String currencyCode) {
    }

    private record SeedEmployee(String employeeCode, String firstName, String lastName, String email,
                                 Long countryId, Long departmentId, Long designationId,
                                 LocalDate joiningDate, String status,
                                 String currencyCode, BigDecimal baseSalary, BigDecimal bonus) {
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.isEnabled()) {
            return;
        }

        Integer existingEmployees = jdbcTemplate.queryForObject("select count(*) from employees", Integer.class);
        if (existingEmployees != null && existingEmployees > 0) {
            log.info("Seed data skipped: employees table already has {} rows", existingEmployees);
            return;
        }

        log.info("Seeding demo data: {} employees...", seedProperties.getEmployeeCount());

        Long hrAdminId = seedHrUsers();
        List<CountryRow> countries = loadCountries();
        List<Long> departmentIds = jdbcTemplate.queryForList("select id from departments", Long.class);
        Map<Long, String> designationNamesById = loadDesignations();

        List<SeedEmployee> employees = generateEmployees(
                seedProperties.getEmployeeCount(), countries, departmentIds, designationNamesById);

        batchInsertEmployees(employees);
        Map<String, Long> employeeIdsByCode = fetchEmployeeIdsByCode();
        batchInsertSalaries(employees, employeeIdsByCode, hrAdminId);

        log.info("Seed data complete: {} employees, {} salary records", employees.size(), employees.size());
    }

    private Long seedHrUsers() {
        Long adminId = ensureHrUser("System Admin", "admin@paysphere.com", HrRole.HR_ADMIN);
        ensureHrUser("HR Manager", "manager@paysphere.com", HrRole.HR_MANAGER);
        ensureHrUser("HR Viewer", "viewer@paysphere.com", HrRole.HR_VIEWER);
        return adminId;
    }

    private Long ensureHrUser(String name, String email, HrRole role) {
        return hrUserRepository.findByEmailIgnoreCase(email)
                .map(HrUser::getId)
                .orElseGet(() -> hrUserRepository.save(HrUser.builder()
                        .name(name)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                        .role(role)
                        .status(UserStatus.ACTIVE)
                        .build()).getId());
    }

    private List<CountryRow> loadCountries() {
        return jdbcTemplate.query("select id, country_code, currency_code from countries",
                (rs, rowNum) -> new CountryRow(rs.getLong("id"), rs.getString("country_code"), rs.getString("currency_code")));
    }

    private Map<Long, String> loadDesignations() {
        Map<Long, String> result = new HashMap<>();
        jdbcTemplate.query("select id, name from designations", rs -> {
            result.put(rs.getLong("id"), rs.getString("name"));
        });
        return result;
    }

    private List<SeedEmployee> generateEmployees(int count,
                                                  List<CountryRow> countries,
                                                  List<Long> departmentIds,
                                                  Map<Long, String> designationNamesById) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Long> designationIds = new ArrayList<>(designationNamesById.keySet());
        List<SeedEmployee> employees = new ArrayList<>(count);

        LocalDate earliestJoining = LocalDate.now().minusYears(8);
        LocalDate latestJoining = LocalDate.now().minusDays(30);
        long joiningDayRange = latestJoining.toEpochDay() - earliestJoining.toEpochDay();

        for (int i = 1; i <= count; i++) {
            String employeeCode = "EMP%06d".formatted(i);
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String email = "%s.%s%d@paysphere-demo.com".formatted(
                    firstName.toLowerCase(), lastName.toLowerCase(), i).replace(" ", "");

            CountryRow country = countries.get(random.nextInt(countries.size()));
            Long departmentId = departmentIds.get(random.nextInt(departmentIds.size()));
            Long designationId = designationIds.get(random.nextInt(designationIds.size()));
            String designationName = designationNamesById.get(designationId);

            LocalDate joiningDate = earliestJoining.plusDays(random.nextLong(joiningDayRange + 1));
            String status = pickStatus(random.nextDouble());

            CountrySalaryRange range = COUNTRY_SALARY_RANGES.getOrDefault(country.code(),
                    new CountrySalaryRange(new BigDecimal("300000"), new BigDecimal("3000000"), new BigDecimal("1000")));
            double multiplier = DESIGNATION_MULTIPLIER.getOrDefault(designationName, 1.0);

            BigDecimal baseSalary = computeBaseSalary(range, multiplier, random);
            BigDecimal bonus = baseSalary.multiply(BigDecimal.valueOf(random.nextDouble(0, 0.20)))
                    .setScale(2, RoundingMode.HALF_UP);

            employees.add(new SeedEmployee(
                    employeeCode, firstName, lastName, email,
                    country.id(), departmentId, designationId,
                    joiningDate, status,
                    country.currencyCode(), baseSalary, bonus
            ));
        }
        return employees;
    }

    private BigDecimal computeBaseSalary(CountrySalaryRange range, double multiplier, ThreadLocalRandom random) {
        BigDecimal spread = range.max().subtract(range.min());
        double normalizedMultiplier = Math.min(multiplier / 2.8, 1.0);
        double jitter = random.nextDouble(0.85, 1.15);

        BigDecimal salary = range.min()
                .add(spread.multiply(BigDecimal.valueOf(normalizedMultiplier)))
                .multiply(BigDecimal.valueOf(jitter));

        BigDecimal roundTo = range.roundTo();
        return salary.divide(roundTo, 0, RoundingMode.HALF_UP).multiply(roundTo);
    }

    private String pickStatus(double roll) {
        double cumulative = 0;
        for (EmployeeStatusWeight weight : STATUS_WEIGHTS) {
            cumulative += weight.weight();
            if (roll <= cumulative) {
                return weight.status();
            }
        }
        return "ACTIVE";
    }

    private void batchInsertEmployees(List<SeedEmployee> employees) {
        String sql = """
                insert into employees
                    (employee_code, first_name, last_name, email, country_id, department_id, designation_id, joining_date, status)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        for (int start = 0; start < employees.size(); start += BATCH_SIZE) {
            List<SeedEmployee> batch = employees.subList(start, Math.min(start + BATCH_SIZE, employees.size()));
            jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, emp) -> {
                ps.setString(1, emp.employeeCode());
                ps.setString(2, emp.firstName());
                ps.setString(3, emp.lastName());
                ps.setString(4, emp.email());
                ps.setLong(5, emp.countryId());
                ps.setLong(6, emp.departmentId());
                ps.setLong(7, emp.designationId());
                ps.setObject(8, emp.joiningDate());
                ps.setString(9, emp.status());
            });
        }
    }

    private Map<String, Long> fetchEmployeeIdsByCode() {
        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query("select id, employee_code from employees", rs -> {
            result.put(rs.getString("employee_code"), rs.getLong("id"));
        });
        return result;
    }

    private void batchInsertSalaries(List<SeedEmployee> employees, Map<String, Long> employeeIdsByCode, Long hrAdminId) {
        String sql = """
                insert into salary_history
                    (employee_id, currency_code, base_salary, bonus, effective_from, effective_to, created_by)
                values (?, ?, ?, ?, ?, null, ?)
                """;

        for (int start = 0; start < employees.size(); start += BATCH_SIZE) {
            List<SeedEmployee> batch = employees.subList(start, Math.min(start + BATCH_SIZE, employees.size()));
            jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, emp) -> {
                ps.setLong(1, employeeIdsByCode.get(emp.employeeCode()));
                ps.setString(2, emp.currencyCode());
                ps.setBigDecimal(3, emp.baseSalary());
                ps.setBigDecimal(4, emp.bonus());
                ps.setObject(5, emp.joiningDate());
                ps.setLong(6, hrAdminId);
            });
        }
    }

    @Component
    @ConfigurationProperties(prefix = "app.seed")
    public static class SeedProperties {
        private boolean enabled = false;
        private int employeeCount = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getEmployeeCount() {
            return employeeCount;
        }

        public void setEmployeeCount(int employeeCount) {
            this.employeeCount = employeeCount;
        }
    }
}
