package com.PaySphere.integration;

import com.PaySphere.entity.Employee;
import com.PaySphere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void create_thenGetById_returnsCreatedEmployee() throws Exception {
        String payload = """
                {"firstName":"John","lastName":"Smith","email":"john.smith@example.com",
                 "countryId":%d,"departmentId":%d,"designationId":%d,"joiningDate":"2024-01-15"}
                """.formatted(country.getId(), department.getId(), designation.getId());

        String response = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").value("EMP000001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/employees/" + id).header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.smith@example.com"))
                .andExpect(jsonPath("$.countryName").value("India"));
    }

    @Test
    void create_withDuplicateEmail_returns409() throws Exception {
        createEmployee("EMP000001", "duplicate@example.com");

        String payload = """
                {"firstName":"John","lastName":"Smith","email":"duplicate@example.com",
                 "countryId":%d,"departmentId":%d,"designationId":%d,"joiningDate":"2024-01-15"}
                """.formatted(country.getId(), department.getId(), designation.getId());

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void create_withInvalidPayload_returns400() throws Exception {
        String payload = """
                {"firstName":"","lastName":"","email":"not-an-email","joiningDate":"2024-01-15"}
                """;

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void getById_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/employees/999999").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_filtersByStatusAndPaginates() throws Exception {
        for (int i = 1; i <= 3; i++) {
            createEmployee("EMP00000" + i, "employee" + i + "@example.com");
        }

        mockMvc.perform(get("/api/employees")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void search_bySearchTerm_matchesEmployeeCode() throws Exception {
        createEmployee("EMP000042", "findme@example.com");
        createEmployee("EMP000043", "other@example.com");

        mockMvc.perform(get("/api/employees")
                        .param("search", "EMP000042")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP000042"));
    }

    @Test
    void update_changesEmployeeFields() throws Exception {
        Employee employee = createEmployee("EMP000050", "before@example.com");

        String payload = """
                {"firstName":"Updated","lastName":"Name","email":"after@example.com",
                 "countryId":%d,"departmentId":%d,"designationId":%d,"joiningDate":"2023-06-01"}
                """.formatted(country.getId(), department.getId(), designation.getId());

        mockMvc.perform(put("/api/employees/" + employee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.email").value("after@example.com"));
    }
}
