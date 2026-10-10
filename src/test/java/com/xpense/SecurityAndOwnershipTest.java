package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rule 1 (every /api call needs a real token) and rule 2 (other people's things look like they don't exist). */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndOwnershipTest extends ApiTestSupport {

    // ------------------------------------------------------------------ authentication

    @ParameterizedTest(name = "GET {0} without a token -> 401")
    @ValueSource(strings = {
            "/api/profile", "/api/wallets", "/api/transactions", "/api/bills",
            "/api/analytics", "/api/analytics/monthly", "/api/analytics/payees", "/api/analytics/alerts",
            "/api/savings-goals", "/api/budgets", "/api/reports", "/api/auth/me"})
    void readEndpointsRejectMissingToken(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void writeEndpointsRejectMissingToken() throws Exception {
        String anyJson = json(body("title", "x", "amount", 10, "type", "income", "name", "x"));
        for (String url : new String[]{"/api/transactions", "/api/wallets", "/api/bills",
                "/api/savings-goals", "/api/budgets", "/api/transactions/transfer"}) {
            mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(anyJson))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(anyJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidOrTamperedTokenIsRejected() throws Exception {
        TestUser user = register(STUDENT);

        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());

        // Change one character in the middle of the signature (not the last one, which can be padding bits)
        String token = user.token();
        int i = token.lastIndexOf('.') + 5;
        char replacement = token.charAt(i) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, i) + replacement + token.substring(i + 1);
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());

        // Token without the Bearer prefix is not accepted either
        mockMvc.perform(get("/api/profile").header("Authorization", token))
                .andExpect(status().isUnauthorized());

        // The real token works and returns this user, not a demo user
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(user.email()));
    }

    @Test
    void registerAndLoginArePublic() throws Exception {
        TestUser user = register(PERSONAL); // register() itself runs without a token

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body("email", user.email(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").value(user.id()));
    }

    // ------------------------------------------------------------------ ownership

    @Test
    void otherUsersWalletIsNotFound() throws Exception {
        TestUser alice = register(PERSONAL);
        TestUser bob = register(PERSONAL);
        String aliceWallet = walletId(alice, 0);
        String originalName = wallets(alice).get(0).get("name").asText();
        addIncome(alice, 1000);
        topUp(alice, aliceWallet, 500, true);

        doGet(bob, "/api/wallets/" + aliceWallet).andExpect(status().isNotFound());
        doPut(bob, "/api/wallets/" + aliceWallet, body("name", "Hacked", "budget_limit", 1))
                .andExpect(status().isNotFound());
        doPost(bob, "/api/wallets/" + aliceWallet + "/topup", body("amount", 100, "from_available", false))
                .andExpect(status().isNotFound());
        doDelete(bob, "/api/wallets/" + aliceWallet).andExpect(status().isNotFound());

        JsonNode wallet = getData(alice, "/api/wallets/" + aliceWallet);
        assertThat(wallet.get("name").asText()).isEqualTo(originalName);
        assertMoney(wallet.get("balance"), "500");
        assertMoney(totalBalance(bob), "0");
        assertThat(texts(wallets(bob), "id")).doesNotContain(aliceWallet);
    }

    @Test
    void otherUsersTransactionIsNotFound() throws Exception {
        TestUser alice = register(STUDENT);
        TestUser bob = register(STUDENT);
        String txId = addIncome(alice, 800);

        doGet(bob, "/api/transactions/" + txId).andExpect(status().isNotFound());
        doDelete(bob, "/api/transactions/" + txId).andExpect(status().isNotFound());

        doGet(alice, "/api/transactions/" + txId).andExpect(status().isOk());
        assertMoney(totalBalance(alice), "800");
        assertThat(texts(getData(bob, "/api/transactions"), "id")).doesNotContain(txId);
    }

    @Test
    void otherUsersSavingsGoalIsNotFound() throws Exception {
        TestUser alice = register(STUDENT);
        TestUser bob = register(STUDENT);
        addIncome(bob, 1000); // bob has money, so only ownership can stop the deposit
        String goalId = data(doPost(alice, "/api/savings-goals", body("title", "Laptop", "target_amount", 50000))
                .andExpect(status().isCreated())).get("id").asText();

        doGet(bob, "/api/savings-goals/" + goalId).andExpect(status().isNotFound());
        doPut(bob, "/api/savings-goals/" + goalId, body("title", "Hacked")).andExpect(status().isNotFound());
        doPost(bob, "/api/savings-goals/" + goalId + "/deposit", body("amount", 100)).andExpect(status().isNotFound());
        doDelete(bob, "/api/savings-goals/" + goalId).andExpect(status().isNotFound());

        JsonNode goal = getData(alice, "/api/savings-goals/" + goalId);
        assertThat(goal.get("title").asText()).isEqualTo("Laptop");
        assertMoney(goal.get("current_amount"), "0");
        assertMoney(totalBalance(bob), "1000");
    }

    @Test
    void otherUsersCategoryLimitIsNotFound() throws Exception {
        TestUser alice = register(STUDENT);
        TestUser bob = register(STUDENT);
        String budgetId = data(doPost(alice, "/api/budgets", body("category", "Food", "limit_amount", 3000))
                .andExpect(status().isCreated())).get("id").asText();

        doGet(bob, "/api/budgets/" + budgetId).andExpect(status().isNotFound());
        doDelete(bob, "/api/budgets/" + budgetId).andExpect(status().isNotFound());

        doGet(alice, "/api/budgets/" + budgetId).andExpect(status().isOk());
    }

    @Test
    void otherUsersBillIsNotFound() throws Exception {
        TestUser alice = register(PERSONAL);
        TestUser bob = register(PERSONAL);
        addIncome(bob, 5000);
        String due = LocalDate.now().plusDays(5).toString();
        String billId = data(doPost(alice, "/api/bills",
                body("name", "Rent", "amount", 1000, "frequency", "monthly", "next_due_date", due))
                .andExpect(status().isCreated())).get("id").asText();

        doPut(bob, "/api/bills/" + billId, body("name", "Hacked")).andExpect(status().isNotFound());
        doPost(bob, "/api/bills/" + billId + "/pay", null).andExpect(status().isNotFound());
        doDelete(bob, "/api/bills/" + billId).andExpect(status().isNotFound());

        JsonNode aliceBills = getData(alice, "/api/bills");
        assertThat(texts(aliceBills, "name")).containsExactly("Rent");
        assertThat(aliceBills.get(0).get("next_due_date").asText()).isEqualTo(due);
        assertThat(getData(bob, "/api/bills").size()).isZero();
        assertThat(transactionCount(bob)).isEqualTo(1);
        assertMoney(totalBalance(bob), "5000");
    }

    @Test
    void cannotSpendFromAnotherUsersWallet() throws Exception {
        TestUser alice = register(PERSONAL);
        TestUser bob = register(PERSONAL);
        String aliceWallet = walletId(alice, 0);
        addIncome(alice, 1000);
        topUp(alice, aliceWallet, 600, true);
        addIncome(bob, 1000); // bob has enough unbudgeted money; only the wallet check can stop this

        doPost(bob, "/api/transactions",
                body("title", "Sneaky", "amount", 200, "type", "expense", "category", "General", "wallet_id", aliceWallet))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        assertMoney(walletBalance(alice, aliceWallet), "600");
        assertMoney(totalBalance(alice), "1000");
        assertMoney(totalBalance(bob), "1000");
        assertThat(transactionCount(bob)).isEqualTo(1);
    }
}
