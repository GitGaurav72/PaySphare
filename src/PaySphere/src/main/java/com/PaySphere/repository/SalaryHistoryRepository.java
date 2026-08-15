package com.PaySphere.repository;

import com.PaySphere.entity.SalaryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SalaryHistoryRepository extends JpaRepository<SalaryHistory, Long> {

    @Query("select s from SalaryHistory s where s.employee.id = :employeeId and s.effectiveTo is null")
    Optional<SalaryHistory> findCurrentByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("select s from SalaryHistory s where s.employee.id = :employeeId order by s.effectiveFrom desc")
    List<SalaryHistory> findAllByEmployeeIdOrderByEffectiveFromDesc(@Param("employeeId") Long employeeId);

    @Query("select avg(s.baseSalary + s.bonus) from SalaryHistory s where s.effectiveTo is null and s.currencyCode = :currency")
    BigDecimal findAverageCurrentTotalCompensationByCurrency(@Param("currency") String currency);

    @Query("select max(s.baseSalary + s.bonus) from SalaryHistory s where s.effectiveTo is null and s.currencyCode = :currency")
    BigDecimal findMaxCurrentTotalCompensationByCurrency(@Param("currency") String currency);

    @Query("select min(s.baseSalary + s.bonus) from SalaryHistory s where s.effectiveTo is null and s.currencyCode = :currency")
    BigDecimal findMinCurrentTotalCompensationByCurrency(@Param("currency") String currency);

    interface CurrencyHeadcount {
        String getCurrencyCode();
        Long getEmployeeCount();
    }

    @Query("""
            select s.currencyCode as currencyCode, count(s) as employeeCount
            from SalaryHistory s
            where s.effectiveTo is null
            group by s.currencyCode
            order by count(s) desc
            """)
    List<CurrencyHeadcount> findHeadcountByCurrency();

    interface DepartmentSalary {
        String getDepartmentName();
        String getCurrencyCode();
        BigDecimal getAverageTotalCompensation();
        Long getEmployeeCount();
    }

    @Query("""
            select d.name as departmentName, s.currencyCode as currencyCode,
                   avg(s.baseSalary + s.bonus) as averageTotalCompensation, count(s) as employeeCount
            from SalaryHistory s join s.employee e join e.department d
            where s.effectiveTo is null
            group by d.name, s.currencyCode
            order by d.name, s.currencyCode
            """)
    List<DepartmentSalary> findSalaryByDepartment();

    interface CountrySalary {
        String getCountryName();
        String getCurrencyCode();
        BigDecimal getAverageTotalCompensation();
        BigDecimal getMinTotalCompensation();
        BigDecimal getMaxTotalCompensation();
        Long getEmployeeCount();
    }

    @Query("""
            select c.name as countryName, s.currencyCode as currencyCode,
                   avg(s.baseSalary + s.bonus) as averageTotalCompensation,
                   min(s.baseSalary + s.bonus) as minTotalCompensation,
                   max(s.baseSalary + s.bonus) as maxTotalCompensation,
                   count(s) as employeeCount
            from SalaryHistory s join s.employee e join e.country c
            where s.effectiveTo is null
            group by c.name, s.currencyCode
            order by c.name
            """)
    List<CountrySalary> findSalaryByCountry();

    interface CurrencyRange {
        String getCurrencyCode();
        BigDecimal getMinTotalCompensation();
        BigDecimal getMaxTotalCompensation();
    }

    @Query("""
            select s.currencyCode as currencyCode,
                   min(s.baseSalary + s.bonus) as minTotalCompensation,
                   max(s.baseSalary + s.bonus) as maxTotalCompensation
            from SalaryHistory s
            where s.effectiveTo is null
            group by s.currencyCode
            """)
    List<CurrencyRange> findCompensationRangeByCurrency();

    @Query("""
            select count(s) from SalaryHistory s
            where s.effectiveTo is null
              and s.currencyCode = :currency
              and (s.baseSalary + s.bonus) >= :low
              and (s.baseSalary + s.bonus) < :high
            """)
    long countByCurrencyAndCompensationRange(@Param("currency") String currency,
                                              @Param("low") BigDecimal low,
                                              @Param("high") BigDecimal high);

    interface TopPaidEmployee {
        Long getEmployeeId();
        String getEmployeeCode();
        String getFirstName();
        String getLastName();
        String getDepartmentName();
        String getCountryName();
        String getCurrencyCode();
        BigDecimal getBaseSalary();
        BigDecimal getBonus();
    }

    @Query("""
            select e.id as employeeId, e.employeeCode as employeeCode, e.firstName as firstName,
                   e.lastName as lastName, d.name as departmentName, c.name as countryName,
                   s.currencyCode as currencyCode, s.baseSalary as baseSalary, s.bonus as bonus
            from SalaryHistory s join s.employee e join e.department d join e.country c
            where s.effectiveTo is null
            order by (s.baseSalary + s.bonus) desc
            """)
    Page<TopPaidEmployee> findTopPaidEmployees(Pageable pageable);
}
