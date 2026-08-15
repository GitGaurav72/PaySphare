package com.PaySphere.service.impl;

import com.PaySphere.dto.dashboard.CountrySalaryResponse;
import com.PaySphere.dto.dashboard.DashboardSummaryResponse;
import com.PaySphere.dto.dashboard.DepartmentCountResponse;
import com.PaySphere.dto.dashboard.DepartmentSalaryResponse;
import com.PaySphere.dto.dashboard.SalaryDistributionBucket;
import com.PaySphere.dto.dashboard.SalaryDistributionResponse;
import com.PaySphere.dto.dashboard.TopPaidEmployeeResponse;
import com.PaySphere.entity.EmployeeStatus;
import com.PaySphere.repository.EmployeeRepository;
import com.PaySphere.repository.SalaryHistoryRepository;
import com.PaySphere.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int DISTRIBUTION_BUCKET_COUNT = 5;

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;

    @Override
    public DashboardSummaryResponse getSummary() {
        long total = employeeRepository.count();
        long active = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long inactive = employeeRepository.countByStatus(EmployeeStatus.INACTIVE);
        long onLeave = employeeRepository.countByStatus(EmployeeStatus.ON_LEAVE);
        long terminated = employeeRepository.countByStatus(EmployeeStatus.TERMINATED);

        List<SalaryHistoryRepository.CurrencyHeadcount> headcounts = salaryHistoryRepository.findHeadcountByCurrency();

        if (headcounts.isEmpty()) {
            return new DashboardSummaryResponse(total, active, inactive, onLeave, terminated, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        String primaryCurrency = headcounts.get(0).getCurrencyCode();
        BigDecimal avg = salaryHistoryRepository.findAverageCurrentTotalCompensationByCurrency(primaryCurrency);
        BigDecimal max = salaryHistoryRepository.findMaxCurrentTotalCompensationByCurrency(primaryCurrency);
        BigDecimal min = salaryHistoryRepository.findMinCurrentTotalCompensationByCurrency(primaryCurrency);

        return new DashboardSummaryResponse(
                total, active, inactive, onLeave, terminated,
                primaryCurrency,
                avg != null ? avg.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                max != null ? max : BigDecimal.ZERO,
                min != null ? min : BigDecimal.ZERO
        );
    }

    @Override
    public List<DepartmentSalaryResponse> getSalaryByDepartment() {
        return salaryHistoryRepository.findSalaryByDepartment().stream()
                .map(p -> new DepartmentSalaryResponse(
                        p.getDepartmentName(),
                        p.getCurrencyCode(),
                        p.getAverageTotalCompensation() != null ? p.getAverageTotalCompensation().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                        p.getEmployeeCount()))
                .toList();
    }

    @Override
    public List<CountrySalaryResponse> getSalaryByCountry() {
        return salaryHistoryRepository.findSalaryByCountry().stream()
                .map(p -> new CountrySalaryResponse(
                        p.getCountryName(),
                        p.getCurrencyCode(),
                        p.getAverageTotalCompensation() != null ? p.getAverageTotalCompensation().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                        p.getMinTotalCompensation(),
                        p.getMaxTotalCompensation(),
                        p.getEmployeeCount()))
                .toList();
    }

    @Override
    public List<DepartmentCountResponse> getEmployeeCountByDepartment() {
        return employeeRepository.countEmployeesByDepartment().stream()
                .map(p -> new DepartmentCountResponse(p.getDepartmentName(), p.getEmployeeCount()))
                .toList();
    }

    @Override
    public List<SalaryDistributionResponse> getSalaryDistribution(String currencyCode) {
        List<SalaryHistoryRepository.CurrencyRange> ranges = salaryHistoryRepository.findCompensationRangeByCurrency();

        List<SalaryDistributionResponse> result = new ArrayList<>();
        for (SalaryHistoryRepository.CurrencyRange range : ranges) {
            if (currencyCode != null && !currencyCode.equalsIgnoreCase(range.getCurrencyCode())) {
                continue;
            }
            result.add(buildDistributionForCurrency(range));
        }
        return result;
    }

    private SalaryDistributionResponse buildDistributionForCurrency(SalaryHistoryRepository.CurrencyRange range) {
        String currency = range.getCurrencyCode();
        BigDecimal min = range.getMinTotalCompensation();
        BigDecimal max = range.getMaxTotalCompensation();

        List<SalaryDistributionBucket> buckets = new ArrayList<>();

        if (min == null || max == null || min.compareTo(max) == 0) {
            long count = min == null ? 0 : salaryHistoryRepository.countByCurrencyAndCompensationRange(
                    currency, min, min.add(BigDecimal.ONE));
            buckets.add(new SalaryDistributionBucket(min, max, count));
            return new SalaryDistributionResponse(currency, buckets);
        }

        BigDecimal width = max.subtract(min).divide(BigDecimal.valueOf(DISTRIBUTION_BUCKET_COUNT), 2, RoundingMode.CEILING);

        BigDecimal low = min;
        for (int i = 0; i < DISTRIBUTION_BUCKET_COUNT; i++) {
            boolean isLastBucket = i == DISTRIBUTION_BUCKET_COUNT - 1;
            BigDecimal high = isLastBucket ? max.add(BigDecimal.ONE) : low.add(width);
            long count = salaryHistoryRepository.countByCurrencyAndCompensationRange(currency, low, high);
            buckets.add(new SalaryDistributionBucket(low, isLastBucket ? max : high, count));
            low = high;
        }

        return new SalaryDistributionResponse(currency, buckets);
    }

    @Override
    public List<TopPaidEmployeeResponse> getTopPaidEmployees(int limit) {
        return salaryHistoryRepository.findTopPaidEmployees(PageRequest.of(0, limit)).stream()
                .map(p -> new TopPaidEmployeeResponse(
                        p.getEmployeeId(),
                        p.getEmployeeCode(),
                        p.getFirstName() + " " + p.getLastName(),
                        p.getDepartmentName(),
                        p.getCountryName(),
                        p.getCurrencyCode(),
                        p.getBaseSalary().add(p.getBonus())))
                .toList();
    }
}
