package com.PaySphere.service;

import com.PaySphere.dto.dashboard.CountrySalaryResponse;
import com.PaySphere.dto.dashboard.DashboardSummaryResponse;
import com.PaySphere.dto.dashboard.DepartmentCountResponse;
import com.PaySphere.dto.dashboard.DepartmentSalaryResponse;
import com.PaySphere.dto.dashboard.SalaryDistributionResponse;
import com.PaySphere.dto.dashboard.TopPaidEmployeeResponse;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getSummary();

    List<DepartmentSalaryResponse> getSalaryByDepartment();

    List<CountrySalaryResponse> getSalaryByCountry();

    List<DepartmentCountResponse> getEmployeeCountByDepartment();

    List<SalaryDistributionResponse> getSalaryDistribution(String currencyCode);

    List<TopPaidEmployeeResponse> getTopPaidEmployees(int limit);
}
