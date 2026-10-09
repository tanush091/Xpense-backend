package com.xpense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpense.dto.LoginRequest;
import com.xpense.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testDemoUserLoginSuccessful() throws Exception {
        LoginRequest loginRequest = new LoginRequest("128003008@sastra.ac.in", "demo123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("128003008@sastra.ac.in"));
    }

    @Test
    void testLoginWithWrongPasswordFails() throws Exception {
        LoginRequest loginRequest = new LoginRequest("128003008@sastra.ac.in", "wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterNewUserSuccessAndIsolation() throws Exception {
        String uniqueEmail = "user_" + System.currentTimeMillis() + "@test.com";
        RegisterRequest registerRequest = new RegisterRequest(uniqueEmail, "password123", "Test User", "Student Account");

        // 1. Register
        String responseContent = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(uniqueEmail))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseContent).get("data").get("token").asText();

        // 2. Fetch authenticated profile
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(uniqueEmail));

        // 3. Verify auto-seeded default wallets exist for this user
        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    void testDuplicateRegistrationFails() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("128003008@sastra.ac.in", "password123", "Duplicate User", "Student Account");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
