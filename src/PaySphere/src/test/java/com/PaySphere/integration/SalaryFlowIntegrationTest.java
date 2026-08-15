package com.PaySphere.integration;

import com.PaySphere.entity.Employee;
import com.PaySphere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalaryFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void creatingNewSalary_closesPreviousCurrentRecord_andKeepsHistory() throws Exception {
        Employee employee = createEmployee("EMP000001", "raise@example.com");

        String firstSalary = """
                {"currencyCode":"INR","baseSalary":1000000,"bonus":50000,"effectiveFrom":"2026-01-01"}
                """;
        mockMvc.perform(post("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(firstSalary))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        mockMvc.perform(get("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseSalary").value(1000000))
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        String raise = """
                {"currencyCode":"INR","baseSalary":1200000,"bonus":60000,"effectiveFrom":"2026-08-01"}
                """;
        mockMvc.perform(post("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(raise))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseSalary").value(1200000))
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        // Current salary must now be the raise, not the original.
        mockMvc.perform(get("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseSalary").value(1200000));

        // History must show exactly two records: the closed original and the current raise.
        mockMvc.perform(get("/api/employees/" + employee.getId() + "/salary-history")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].baseSalary").value(1200000))
                .andExpect(jsonPath("$[0].effectiveTo").doesNotExist())
                .andExpect(jsonPath("$[1].baseSalary").value(1000000))
                .andExpect(jsonPath("$[1].effectiveFrom").value("2026-01-01"))
                .andExpect(jsonPath("$[1].effectiveTo").value("2026-07-31"));
    }

    @Test
    void creatingSalary_withDateNotAfterCurrent_returns400() throws Exception {
        Employee employee = createEmployee("EMP000002", "reject@example.com");

        String firstSalary = """
                {"currencyCode":"INR","baseSalary":1000000,"bonus":0,"effectiveFrom":"2026-01-01"}
                """;
        mockMvc.perform(post("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(firstSalary))
                .andExpect(status().isCreated());

        String backdated = """
                {"currencyCode":"INR","baseSalary":1100000,"bonus":0,"effectiveFrom":"2026-01-01"}
                """;
        mockMvc.perform(post("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(backdated))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingSalary_withNegativeBaseSalary_returns400() throws Exception {
        Employee employee = createEmployee("EMP000003", "negative@example.com");

        String invalid = """
                {"currencyCode":"INR","baseSalary":-100,"bonus":0,"effectiveFrom":"2026-01-01"}
                """;
        mockMvc.perform(post("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hrViewer_cannotCreateSalaryChange_returns403() throws Exception {
        Employee employee = createEmployee("EMP000004", "viewer-blocked@example.com");

        String payload = """
                {"currencyCode":"INR","baseSalary":1000000,"bonus":0,"effectiveFrom":"2026-01-01"}
                """;
        mockMvc.perform(post("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCurrentSalary_withNoSalaryRecords_returns404() throws Exception {
        Employee employee = createEmployee("EMP000005", "nosalary@example.com");

        mockMvc.perform(get("/api/employees/" + employee.getId() + "/salary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }
}
