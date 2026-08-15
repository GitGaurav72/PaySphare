package com.PaySphere.controller;

import com.PaySphere.dto.dashboard.CountrySalaryResponse;
import com.PaySphere.dto.dashboard.DashboardSummaryResponse;
import com.PaySphere.dto.dashboard.DepartmentCountResponse;
import com.PaySphere.dto.dashboard.DepartmentSalaryResponse;
import com.PaySphere.dto.dashboard.SalaryDistributionResponse;
import com.PaySphere.dto.dashboard.TopPaidEmployeeResponse;
import com.PaySphere.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/salary-by-department")
    public ResponseEntity<List<DepartmentSalaryResponse>> getSalaryByDepartment() {
        return ResponseEntity.ok(dashboardService.getSalaryByDepartment());
    }

    @GetMapping("/salary-by-country")
    public ResponseEntity<List<CountrySalaryResponse>> getSalaryByCountry() {
        return ResponseEntity.ok(dashboardService.getSalaryByCountry());
    }

    @GetMapping("/employee-count-by-department")
    public ResponseEntity<List<DepartmentCountResponse>> getEmployeeCountByDepartment() {
        return ResponseEntity.ok(dashboardService.getEmployeeCountByDepartment());
    }

    @GetMapping("/salary-distribution")
    public ResponseEntity<List<SalaryDistributionResponse>> getSalaryDistribution(
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(dashboardService.getSalaryDistribution(currency));
    }

    @GetMapping("/top-paid-employees")
    public ResponseEntity<List<TopPaidEmployeeResponse>> getTopPaidEmployees(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getTopPaidEmployees(limit));
    }
}
