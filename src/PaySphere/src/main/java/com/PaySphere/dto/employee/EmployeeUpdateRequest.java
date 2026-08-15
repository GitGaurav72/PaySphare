package com.PaySphere.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeUpdateRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotNull(message = "Country is required")
        Long countryId,

        @NotNull(message = "Department is required")
        Long departmentId,

        @NotNull(message = "Designation is required")
        Long designationId,

        @NotNull(message = "Joining date is required")
        LocalDate joiningDate
) {
}
