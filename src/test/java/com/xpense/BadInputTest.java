package com.xpense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpense.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Malformed input must give a 400 with a plain message, never a 500 or internal parser text. */
@SpringBootTest
@AutoConfigureMockMvc
class BadInputTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void signUp() throws Exception {
        RegisterRequest req = new RegisterRequest("bad_input_" + System.nanoTime() + "@test.com", "password123", "Bad Input", "Personal Account");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("data").get("token").asText();
    }

    @Test
    void textForANumberInTheQueryIs400() throws Exception {
        mockMvc.perform(get("/api/analytics/monthly?months=abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/api/analytics/payees?limit=abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void textForAnAmountIs400WithPlainMessage() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lunch\",\"amount\":\"abc\",\"type\":\"expense\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(not(containsString("JSON"))));
    }

    @Test
    void badDateIs400() throws Exception {
        mockMvc.perform(post("/api/bills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rent\",\"amount\":1000,\"frequency\":\"monthly\",\"next_due_date\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brokenJsonIs400() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(not(containsString("Unexpected character"))));
    }
}
