package com.example.demo.Integration;

import com.example.demo.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerUser_ShouldReturn200AndJson() throws Exception {
        User newUser = new User();
        newUser.setName("Integration Test User");
        newUser.setEmail("integration_" + UUID.randomUUID() + "@mail.com"); // ✅ unique each run
        newUser.setPassword("secret123");
        newUser.setInstrument("Piano");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void loginUser_WrongPassword_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "nonexistent_user@mail.com",
                              "password": "wrongpass"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid credentials"));
    }
}
