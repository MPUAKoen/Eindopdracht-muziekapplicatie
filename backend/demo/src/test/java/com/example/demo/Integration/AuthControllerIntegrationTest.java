package com.example.demo.Integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerUser_ShouldReturn200AndJwtPayload() throws Exception {
        Map<String, Object> registerPayload = Map.of(
                "name", "Integration Test User",
                "email", "integration_" + UUID.randomUUID() + "@mail.com",
                "password", "secret123",
                "instrument", "Piano"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").exists());
    }

    @Test
    void registerUser_MissingRequiredFields_ShouldReturnValidationDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "",
                              "email": "",
                              "password": ""
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Validation failed")))
                .andExpect(jsonPath("$.validationErrors[*].field").value(org.hamcrest.Matchers.hasItems(
                        "name",
                        "email",
                        "password"
                )));
    }

    @Test
    void loginUser_WrongPassword_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "nonexistent_user@mail.com",
                              "password": "wrongpass"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllUsers_AsRegularUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user("student@mail.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
