package com.PaySphere.integration;

import com.PaySphere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityRbacIntegrationTest extends AbstractIntegrationTest {

    private static final String EMPLOYEE_PAYLOAD = """
            {"firstName":"Jane","lastName":"Doe","email":"jane.doe@example.com",
             "countryId":%d,"departmentId":%d,"designationId":%d,"joiningDate":"2024-01-15"}
            """;

    @Test
    void unauthenticatedRequest_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/employees").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hrViewer_canReadEmployees() throws Exception {
        mockMvc.perform(get("/api/employees").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    void hrViewer_cannotCreateEmployee_returns403() throws Exception {
        String payload = EMPLOYEE_PAYLOAD.formatted(country.getId(), department.getId(), designation.getId());

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrManager_canCreateEmployee_returns201() throws Exception {
        String payload = EMPLOYEE_PAYLOAD.formatted(country.getId(), department.getId(), designation.getId());

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void hrManager_cannotManageHrUsers_returns403() throws Exception {
        mockMvc.perform(get("/api/hr-users").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrViewer_cannotManageHrUsers_returns403() throws Exception {
        mockMvc.perform(get("/api/hr-users").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrAdmin_canManageHrUsers_returns200() throws Exception {
        mockMvc.perform(get("/api/hr-users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
