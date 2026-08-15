package com.PaySphere.dto.salary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryCreateRequest(

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 10, message = "Currency code must be between 3 and 10 characters")
        String currencyCode,

        @NotNull(message = "Base salary is required")
        @PositiveOrZero(message = "Base salary must be zero or positive")
        BigDecimal baseSalary,

        @PositiveOrZero(message = "Bonus must be zero or positive")
        BigDecimal bonus,

        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom
) {
}
