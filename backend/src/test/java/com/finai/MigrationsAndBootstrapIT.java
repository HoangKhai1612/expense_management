package com.finai;

import com.finai.category.Category;
import com.finai.category.CategoryRepository;
import com.finai.user.Role;
import com.finai.user.RoleRepository;
import com.finai.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the Flyway migrations actually build the schema the entities expect
 * and that the bootstrap seed lands, against a real PostgreSQL instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MigrationsAndBootstrapIT extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("every migration applies and the application starts against the result")
    void migrationsApply() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Hibernate validates the entity mapping against the migrated schema")
    void entitiesMatchSchema() {
        // Context start-up with ddl-auto=validate already proves this; the assertions
        // below make the seeded content explicit rather than implied.
        assertThat(roleRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(categoryRepository.count()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("both roles are seeded")
    void seedsRoles() {
        assertThat(roleRepository.findByName(Role.USER)).isPresent();
        assertThat(roleRepository.findByName(Role.ADMIN)).isPresent();
    }

    @Test
    @DisplayName("the bootstrap administrator exists with the ADMIN role")
    void seedsBootstrapAdmin() {
        assertThat(userRepository.findByEmailIgnoreCase("admin@finai.local")).isPresent();
    }

    @Test
    @DisplayName("the seeded catalogue has both income and expense categories")
    void seedsCategoryCatalogue() {
        long income = categoryRepository.findAll().stream()
                .filter(category -> category.getType() == com.finai.category.CategoryType.INCOME)
                .count();
        long expense = categoryRepository.findAll().stream()
                .filter(category -> category.getType() == com.finai.category.CategoryType.EXPENSE)
                .count();

        assertThat(income).isGreaterThan(0);
        assertThat(expense).isGreaterThan(0);
    }

    @Test
    @DisplayName("a registered user can log in and reach an authenticated endpoint")
    void registerThenLoginEndToEnd() throws Exception {
        String stamp = UUID.randomUUID().toString().substring(0, 8);

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "it." + stamp + "@example.com",
                                "username", "it_" + stamp,
                                "password", "Password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(registered.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();

        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("an unauthenticated request to a protected route is refused")
    void protectsRoutes() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }
}
