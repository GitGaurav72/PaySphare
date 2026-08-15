package com.PaySphere.service.impl;

import com.PaySphere.entity.Employee;
import com.PaySphere.entity.EmployeeStatus;
import com.PaySphere.entity.SalaryHistory;
import com.PaySphere.exception.ResourceNotFoundException;
import com.PaySphere.repository.CountryRepository;
import com.PaySphere.repository.DepartmentRepository;
import com.PaySphere.repository.DesignationRepository;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.repository.SalaryHistoryRepository;
import com.PaySphere.service.ExcelReportService;
import com.PaySphere.specification.EmployeeSpecifications;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelReportServiceImpl implements ExcelReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int TEMPLATE_DATA_ROWS = 500;

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final CountryRepository countryRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateEmployeeReport(String search,
                                          Long countryId,
                                          Long departmentId,
                                          Long designationId,
                                          EmployeeStatus status) {
        List<Employee> employees = employeeRepository.findAll(
                EmployeeSpecifications.withFilters(search, countryId, departmentId, designationId, status),
                Sort.by("employeeCode"));

        // SXSSFWorkbook streams rows to disk instead of holding the whole sheet in memory,
        // since an unfiltered export can be up to the full ~10,000-employee dataset.
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = workbook.createSheet("Employees");
            CellStyle headerStyle = headerStyle(workbook);

            String[] headers = {"Employee Code", "First Name", "Last Name", "Email", "Country",
                    "Department", "Designation", "Joining Date", "Status"};
            writeHeaderRow(sheet, headerStyle, headers);

            int rowIdx = 1;
            for (Employee e : employees) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(e.getEmployeeCode());
                row.createCell(col++).setCellValue(e.getFirstName());
                row.createCell(col++).setCellValue(e.getLastName());
                row.createCell(col++).setCellValue(e.getEmail());
                row.createCell(col++).setCellValue(e.getCountry().getName());
                row.createCell(col++).setCellValue(e.getDepartment().getName());
                row.createCell(col++).setCellValue(e.getDesignation().getName());
                row.createCell(col++).setCellValue(e.getJoiningDate().format(DATE_FMT));
                row.createCell(col).setCellValue(e.getStatus().name());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.trackColumnForAutoSizing(i);
                sheet.autoSizeColumn(i);
            }

            byte[] bytes = toBytes(workbook);
            workbook.dispose();
            return bytes;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate employee report", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateSalaryHistoryReport(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        List<SalaryHistory> history = salaryHistoryRepository.findAllByEmployeeIdOrderByEffectiveFromDesc(employeeId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Salary History");
            CellStyle headerStyle = headerStyle(workbook);

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue(
                    "Salary history — %s %s (%s)".formatted(employee.getFirstName(), employee.getLastName(), employee.getEmployeeCode()));

            String[] headers = {"Effective From", "Effective To", "Currency", "Base Salary", "Bonus",
                    "Total", "Payment Status", "Paid At", "Created By", "Created At"};
            writeHeaderRow(sheet, headerStyle, headers, 2);

            int rowIdx = 3;
            for (SalaryHistory s : history) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(s.getEffectiveFrom().format(DATE_FMT));
                row.createCell(col++).setCellValue(s.getEffectiveTo() != null ? s.getEffectiveTo().format(DATE_FMT) : "Current");
                row.createCell(col++).setCellValue(s.getCurrencyCode());
                row.createCell(col++).setCellValue(s.getBaseSalary().doubleValue());
                row.createCell(col++).setCellValue(s.getBonus().doubleValue());
                row.createCell(col++).setCellValue(total(s).doubleValue());
                row.createCell(col++).setCellValue(s.getPaymentStatus().name());
                row.createCell(col++).setCellValue(s.getPaidAt() != null ? s.getPaidAt().format(DATETIME_FMT) : "");
                row.createCell(col++).setCellValue(s.getCreatedBy().getName());
                row.createCell(col).setCellValue(s.getCreatedAt().format(DATETIME_FMT));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return toBytes(workbook);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate salary history report", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePayslip(Long employeeId, Long salaryId) {
        SalaryHistory salary = salaryHistoryRepository.findById(salaryId)
                .filter(s -> s.getEmployee().getId().equals(employeeId))
                .orElseThrow(() -> new ResourceNotFoundException("Salary record not found with id: " + salaryId));
        return generatePayslip(salary);
    }

    @Override
    public byte[] generatePayslip(SalaryHistory salary) {
        Employee employee = salary.getEmployee();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Payslip");
            CellStyle labelStyle = headerStyle(workbook);

            String period = salary.getEffectiveFrom().format(DATE_FMT) + " to "
                    + (salary.getEffectiveTo() != null ? salary.getEffectiveTo().format(DATE_FMT) : "Current");

            String[][] rows = {
                    {"PaySphere — Payslip", ""},
                    {"", ""},
                    {"Employee", employee.getFirstName() + " " + employee.getLastName()},
                    {"Employee Code", employee.getEmployeeCode()},
                    {"Department", employee.getDepartment().getName()},
                    {"Designation", employee.getDesignation().getName()},
                    {"Country", employee.getCountry().getName()},
                    {"Pay Period", period},
                    {"", ""},
                    {"Currency", salary.getCurrencyCode()},
                    {"Base Salary", salary.getBaseSalary().toPlainString()},
                    {"Bonus", salary.getBonus().toPlainString()},
                    {"Total", total(salary).toPlainString()},
                    {"", ""},
                    {"Payment Status", salary.getPaymentStatus().name()},
                    {"Paid At", salary.getPaidAt() != null ? salary.getPaidAt().format(DATETIME_FMT) : "-"}
            };

            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i);
                Cell labelCell = row.createCell(0);
                labelCell.setCellValue(rows[i][0]);
                if (!rows[i][0].isBlank()) {
                    labelCell.setCellStyle(labelStyle);
                }
                row.createCell(1).setCellValue(rows[i][1]);
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            return toBytes(workbook);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate payslip", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateEmployeeBulkUploadTemplate() {
        List<String> countryNames = countryRepository.findAll().stream()
                .map(c -> c.getName()).sorted(Comparator.naturalOrder()).toList();
        List<String> departmentNames = departmentRepository.findAll().stream()
                .map(d -> d.getName()).sorted(Comparator.naturalOrder()).toList();
        List<String> designationNames = designationRepository.findAll().stream()
                .map(d -> d.getName()).sorted(Comparator.naturalOrder()).toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Explicit inline-list validation is limited to 255 characters total, which the
            // designation list alone can exceed — so the valid values live on a separate sheet
            // and each dropdown references that range by formula instead.
            Sheet listsSheet = workbook.createSheet("Lists");
            writeListColumn(listsSheet, 0, countryNames);
            writeListColumn(listsSheet, 1, departmentNames);
            writeListColumn(listsSheet, 2, designationNames);

            Sheet sheet = workbook.createSheet("Employees");
            CellStyle headerStyle = headerStyle(workbook);

            String[] headers = {"First Name", "Last Name", "Email", "Country", "Department",
                    "Designation", "Joining Date (YYYY-MM-DD)"};
            writeHeaderRow(sheet, headerStyle, headers);
            sheet.createFreezePane(0, 1);

            addDropdownValidation(sheet, 3, "Lists", 0, countryNames.size());
            addDropdownValidation(sheet, 4, "Lists", 1, departmentNames.size());
            addDropdownValidation(sheet, 5, "Lists", 2, designationNames.size());

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(6, 6000);

            workbook.setSheetOrder("Employees", 0);
            workbook.setActiveSheet(0);

            return toBytes(workbook);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate bulk upload template", ex);
        }
    }

    private void writeListColumn(Sheet sheet, int columnIndex, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                row = sheet.createRow(i);
            }
            row.createCell(columnIndex).setCellValue(values.get(i));
        }
    }

    private void addDropdownValidation(Sheet sheet, int columnIndex, String listSheetName, int listColumnIndex, int listSize) {
        String columnLetter = org.apache.poi.ss.util.CellReference.convertNumToColString(listColumnIndex);
        String formula = "'%s'!$%s$1:$%s$%d".formatted(listSheetName, columnLetter, columnLetter, Math.max(listSize, 1));

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        CellRangeAddressList addressList = new CellRangeAddressList(1, TEMPLATE_DATA_ROWS, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.setSuppressDropDownArrow(true);
        sheet.addValidationData(validation);
    }

    private BigDecimal total(SalaryHistory salary) {
        return salary.getBaseSalary().add(salary.getBonus());
    }

    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle, String[] headers) {
        writeHeaderRow(sheet, headerStyle, headers, 0);
    }

    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle, String[] headers, int rowIndex) {
        Row headerRow = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
