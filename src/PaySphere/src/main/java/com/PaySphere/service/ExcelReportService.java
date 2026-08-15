package com.PaySphere.service;

import com.PaySphere.entity.EmployeeStatus;
import com.PaySphere.entity.SalaryHistory;

public interface ExcelReportService {

    byte[] generateEmployeeReport(String search,
                                   Long countryId,
                                   Long departmentId,
                                   Long designationId,
                                   EmployeeStatus status);

    byte[] generateSalaryHistoryReport(Long employeeId);

    byte[] generatePayslip(SalaryHistory salary);

    byte[] generatePayslip(Long employeeId, Long salaryId);

    byte[] generateEmployeeBulkUploadTemplate();
}
