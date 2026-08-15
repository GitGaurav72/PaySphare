package com.PaySphere.specification;

import com.PaySphere.entity.Employee;
import com.PaySphere.entity.EmployeeStatus;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> withFilters(String search,
                                                        Long countryId,
                                                        Long departmentId,
                                                        Long designationId,
                                                        EmployeeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // country/department/designation are mandatory (NOT NULL) associations, so an
            // inner join is always safe here — it also lets the same join back a fetch
            // (eager-loading the association) without Hibernate creating a duplicate join.
            // Fetching is skipped for the count query Spring Data issues alongside the data
            // query, since fetch + count(*) is invalid JPQL.
            boolean isDataQuery = Long.class != query.getResultType() && long.class != query.getResultType();

            Join<Employee, Object> countryJoin = isDataQuery
                    ? castToJoin(root.fetch("country", JoinType.INNER))
                    : root.join("country", JoinType.INNER);
            Join<Employee, Object> departmentJoin = isDataQuery
                    ? castToJoin(root.fetch("department", JoinType.INNER))
                    : root.join("department", JoinType.INNER);
            Join<Employee, Object> designationJoin = isDataQuery
                    ? castToJoin(root.fetch("designation", JoinType.INNER))
                    : root.join("designation", JoinType.INNER);

            if (isDataQuery) {
                query.distinct(true);
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("employeeCode")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            if (countryId != null) {
                predicates.add(cb.equal(countryJoin.get("id"), countryId));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(departmentJoin.get("id"), departmentId));
            }

            if (designationId != null) {
                predicates.add(cb.equal(designationJoin.get("id"), designationId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings("unchecked")
    private static Join<Employee, Object> castToJoin(Fetch<Employee, Object> fetch) {
        return (Join<Employee, Object>) fetch;
    }
}
