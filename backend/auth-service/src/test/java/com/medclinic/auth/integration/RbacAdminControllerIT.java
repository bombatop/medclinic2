package com.medclinic.auth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rbac_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class RbacAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", authorities = {"PERM_users.read_all"})
    void shouldExposePermissionCatalog() throws Exception {
        mockMvc.perform(get("/auth/rbac/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"PERM_users.manage_roles", "PERM_users.read_all"})
    void shouldCreateRoleAndManageRolePermissions() throws Exception {
        String createBody = """
                {
                  "code": "IT_ROLE",
                  "name": "Integration Test Role",
                  "description": "Created by test",
                  "active": true
                }
                """;

        String createdRoleJson = mockMvc.perform(post("/auth/rbac/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("IT_ROLE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdRoleNode = objectMapper.readTree(createdRoleJson);
        long roleId = createdRoleNode.get("id").asLong();

        String updatePermissionsBody = """
                {
                  "permissions": ["appointment.read_all", "employee.read_all"]
                }
                """;

        mockMvc.perform(put("/auth/rbac/roles/{id}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePermissionsBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0]").exists());

        mockMvc.perform(get("/auth/rbac/roles/{id}/permissions", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(roleId))
                .andExpect(jsonPath("$.permissions").isArray());
    }
}
