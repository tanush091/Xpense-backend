package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for the MockMvc API tests: register a fresh user, call the API with
 * their token, and read money values out of the standard {@code {success, message, data}} envelope.
 * Concrete test classes carry {@code @SpringBootTest @AutoConfigureMockMvc} so they share one cached context.
 */
abstract class ApiTestSupport {

    static final String STUDENT = "Student Account";
    static final String PERSONAL = "Personal Account";
    static final String CORPORATE = "Corporate SaaS";
    static final String PASSWORD = "secret123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** A freshly registered user and their bearer token. */
    protected record TestUser(String token, String id, String email) {
    }

    // ---------------------------------------------------------------- users

    protected TestUser register(String accountType) throws Exception {
        String email = "t" + System.nanoTime() + "_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String content = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body("email", email, "password", PASSWORD,
                                "full_name", "Test User", "account_type", accountType))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(content).get("data");
        return new TestUser(data.get("token").asText(), data.get("user").get("id").asText(), email);
    }

    // ---------------------------------------------------------------- raw calls

    protected ResultActions doGet(TestUser user, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer(user)));
    }

    protected ResultActions doPost(TestUser user, String url, Object body) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body == null ? Map.of() : body)));
    }

    protected ResultActions doPut(TestUser user, String url, Object body) throws Exception {
        return mockMvc.perform(put(url).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    protected ResultActions doDelete(TestUser user, String url) throws Exception {
        return mockMvc.perform(delete(url).header("Authorization", bearer(user)));
    }

    /** The {@code data} part of a response that has already been checked. */
    protected JsonNode data(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("data");
    }

    /** GET that must succeed; returns {@code data}. */
    protected JsonNode getData(TestUser user, String url) throws Exception {
        return data(doGet(user, url).andExpect(status().isOk()));
    }

    // ---------------------------------------------------------------- money helpers

    protected BigDecimal totalBalance(TestUser user) throws Exception {
        return getData(user, "/api/profile").get("total_balance").decimalValue();
    }

    protected JsonNode wallets(TestUser user) throws Exception {
        return getData(user, "/api/wallets");
    }

    protected String walletId(TestUser user, int index) throws Exception {
        return wallets(user).get(index).get("id").asText();
    }

    protected BigDecimal walletBalance(TestUser user, String walletId) throws Exception {
        return getData(user, "/api/wallets/" + walletId).get("balance").decimalValue();
    }

    /** Money not in any budget yet: total balance minus the sum of wallet balances (never below 0). */
    protected BigDecimal unbudgeted(TestUser user) throws Exception {
        BigDecimal inWallets = BigDecimal.ZERO;
        for (JsonNode w : wallets(user)) {
            inWallets = inWallets.add(w.get("balance").decimalValue());
        }
        return totalBalance(user).subtract(inWallets).max(BigDecimal.ZERO);
    }

    protected String addIncome(TestUser user, int amount) throws Exception {
        JsonNode tx = data(doPost(user, "/api/transactions",
                body("title", "Income", "amount", amount, "type", "income", "category", "Salary"))
                .andExpect(status().isCreated()));
        return tx.get("id").asText();
    }

    /** Records an expense; {@code walletId} may be null to pay from unbudgeted money. */
    protected String addExpense(TestUser user, String walletId, int amount, String title) throws Exception {
        JsonNode tx = data(doPost(user, "/api/transactions",
                body("title", title, "amount", amount, "type", "expense", "category", "General", "wallet_id", walletId))
                .andExpect(status().isCreated()));
        return tx.get("id").asText();
    }

    protected void topUp(TestUser user, String walletId, int amount, boolean fromAvailable) throws Exception {
        doPost(user, "/api/wallets/" + walletId + "/topup", body("amount", amount, "from_available", fromAvailable))
                .andExpect(status().isOk());
    }

    protected int transactionCount(TestUser user) throws Exception {
        return getData(user, "/api/transactions").size();
    }

    protected static void assertMoney(BigDecimal actual, String expected) {
        assertThat(actual).as("money value").isEqualByComparingTo(expected);
    }

    protected static void assertMoney(JsonNode node, String expected) {
        assertThat(node).as("money node").isNotNull();
        assertThat(node.isNull()).as("money node is null").isFalse();
        assertMoney(node.decimalValue(), expected);
    }

    protected static List<String> texts(JsonNode array, String field) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get(field).asText()));
        return out;
    }

    // ---------------------------------------------------------------- json

    /** Builds a JSON body from key/value pairs; null values are left out. */
    protected static Map<String, Object> body(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                map.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        return map;
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String bearer(TestUser user) {
        return "Bearer " + user.token();
    }
}
