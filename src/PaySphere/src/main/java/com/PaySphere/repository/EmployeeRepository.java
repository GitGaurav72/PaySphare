package com.PaySphere.repository;

import com.PaySphere.entity.Employee;
import com.PaySphere.entity.EmployeeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    @EntityGraph(attributePaths = {"country", "department", "designation"})
    Optional<Employee> findById(Long id);

    @Query("select max(cast(substring(e.employeeCode, 4) as int)) from Employee e")
    Optional<Integer> findMaxEmployeeCodeSequence();

    /**
     * Locks the employee row for the duration of the transaction. Used to serialize
     * concurrent salary changes for the same employee — see SalaryServiceImpl — since
     * two transactions both reading "no current salary row" at once would otherwise be
     * able to insert two current (effective_to IS NULL) salary records.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(EmployeeStatus status);

    interface EmployeeCountByDepartment {
        String getDepartmentName();
        Long getEmployeeCount();
    }

    @Query("""
            select d.name as departmentName, count(e) as employeeCount
            from Employee e join e.department d
            group by d.name
            order by count(e) desc
            """)
    List<EmployeeCountByDepartment> countEmployeesByDepartment();
}
