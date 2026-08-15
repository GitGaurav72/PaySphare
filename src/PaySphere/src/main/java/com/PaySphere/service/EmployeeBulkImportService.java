package com.PaySphere.service;

import com.PaySphere.dto.employee.BulkUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface EmployeeBulkImportService {

    BulkUploadResponse importEmployees(MultipartFile file);
}
