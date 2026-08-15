package com.PaySphere.service.impl;

import com.PaySphere.dto.employee.BulkUploadResponse;
import com.PaySphere.dto.employee.BulkUploadRowResult;
import com.PaySphere.entity.Country;
import com.PaySphere.entity.Department;
import com.PaySphere.entity.Designation;
import com.PaySphere.entity.Employee;
import com.PaySphere.entity.EmployeeStatus;
import com.PaySphere.exception.BadRequestException;
import com.PaySphere.repository.CountryRepository;
import com.PaySphere.repository.DepartmentRepository;
import com.PaySphere.repository.DesignationRepository;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.service.EmployeeBulkImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmployeeBulkImportServiceImpl implements EmployeeBulkImportService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeBulkImportServiceImpl.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String EMPLOYEE_CODE_FORMAT = "EMP%06d";

    private final EmployeeRepository employeeRepository;
    private final CountryRepository countryRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    @Override
    @Transactional
    public BulkUploadResponse importEmployees(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        Map<String, Country> countriesByName = indexByLowerName(countryRepository.findAll(), Country::getName);
        Map<String, Department> departmentsByName = indexByLowerName(departmentRepository.findAll(), Department::getName);
        Map<String, Designation> designationsByName = indexByLowerName(designationRepository.findAll(), Designation::getName);

        int nextSequence = employeeRepository.findMaxEmployeeCodeSequence().orElse(0) + 1;
        Set<String> emailsSeenInFile = new HashSet<>();

        List<BulkUploadRowResult> results = new ArrayList<>();

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowBlank(row)) {
                    continue;
                }

                int excelRowNumber = rowIdx + 1;
                String firstName = readString(row, 0);
                String lastName = readString(row, 1);
                String email = readString(row, 2);
                String countryName = readString(row, 3);
                String departmentName = readString(row, 4);
                String designationName = readString(row, 5);

                List<String> errors = new ArrayList<>();

                if (firstName.isBlank()) errors.add("First name is required");
                if (lastName.isBlank()) errors.add("Last name is required");
                if (email.isBlank()) {
                    errors.add("Email is required");
                } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                    errors.add("Email is not valid: '" + email + "'");
                } else if (emailsSeenInFile.contains(email.toLowerCase(Locale.ROOT))) {
                    errors.add("Duplicate email within this file: '" + email + "'");
                } else if (employeeRepository.existsByEmailIgnoreCase(email)) {
                    errors.add("An employee with email '" + email + "' already exists");
                }

                Country country = countriesByName.get(countryName.toLowerCase(Locale.ROOT));
                if (country == null) errors.add("Unknown country: '" + countryName + "'");

                Department department = departmentsByName.get(departmentName.toLowerCase(Locale.ROOT));
                if (department == null) errors.add("Unknown department: '" + departmentName + "'");

                Designation designation = designationsByName.get(designationName.toLowerCase(Locale.ROOT));
                if (designation == null) errors.add("Unknown designation: '" + designationName + "'");

                LocalDate joiningDate = null;
                try {
                    joiningDate = readDate(row, 6);
                    if (joiningDate == null) errors.add("Joining date is required (format: YYYY-MM-DD)");
                } catch (DateTimeParseException ex) {
                    errors.add("Joining date is not a valid date (expected YYYY-MM-DD)");
                }

                if (!errors.isEmpty()) {
                    results.add(new BulkUploadRowResult(excelRowNumber, email, false, null, String.join("; ", errors)));
                    continue;
                }

                try {
                    String employeeCode = EMPLOYEE_CODE_FORMAT.formatted(nextSequence++);
                    Employee employee = Employee.builder()
                            .employeeCode(employeeCode)
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .country(country)
                            .department(department)
                            .designation(designation)
                            .joiningDate(joiningDate)
                            .status(EmployeeStatus.ACTIVE)
                            .build();
                    employeeRepository.save(employee);
                    emailsSeenInFile.add(email.toLowerCase(Locale.ROOT));
                    results.add(new BulkUploadRowResult(excelRowNumber, email, true, employeeCode, "Created"));
                } catch (Exception ex) {
                    log.warn("Bulk import row {} failed: {}", excelRowNumber, ex.getMessage());
                    results.add(new BulkUploadRowResult(excelRowNumber, email, false, null, "Failed to save: " + ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded file — make sure it's a valid .xlsx file");
        }

        long successCount = results.stream().filter(BulkUploadRowResult::success).count();
        return new BulkUploadResponse(results.size(), (int) successCount, results.size() - (int) successCount, results);
    }

    private <T> Map<String, T> indexByLowerName(List<T> items, java.util.function.Function<T, String> nameFn) {
        Map<String, T> map = new HashMap<>();
        for (T item : items) {
            map.put(nameFn.apply(item).toLowerCase(Locale.ROOT), item);
        }
        return map;
    }

    private boolean isRowBlank(Row row) {
        for (int col = 0; col < 7; col++) {
            if (!readString(row, col).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LocalDate readDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = readString(row, col);
        if (text.isBlank()) return null;
        return LocalDate.parse(text);
    }
}
