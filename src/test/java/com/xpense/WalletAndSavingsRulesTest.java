package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rules 4–6: wallet top-ups, wallet create/edit, and savings goals. */
@SpringBootTest
@AutoConfigureMockMvc
class WalletAndSavingsRulesTest extends ApiTestSupport {

    // ------------------------------------------------------------------ top-up (rule 4)

    @Test
    void topUpFromAvailableMovesUnbudgetedMoneyWithoutChangingTotal() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);

        JsonNode updated = data(doPost(user, "/api/wallets/" + wallet + "/topup", body("amount", 400, "from_available", true))
                .andExpect(status().isOk()));

        assertMoney(updated.get("balance"), "400");
        assertMoney(walletBalance(user, wallet), "400");
        assertMoney(totalBalance(user), "1000");
        assertMoney(unbudgeted(user), "600");
    }

    @Test
    void topUpFromAvailableIsRejectedWhenNotEnoughUnbudgetedMoney() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 0);
        addIncome(user, 300);

        doPost(user, "/api/wallets/" + wallet + "/topup", body("amount", 500, "from_available", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertMoney(walletBalance(user, wallet), "0");
        assertMoney(totalBalance(user), "300");
    }

    @Test
    void topUpFromAvailableCannotReuseMoneyAlreadyInAnotherWallet() throws Exception {
        TestUser user = register(PERSONAL);
        String first = walletId(user, 0);
        String second = walletId(user, 1);
        addIncome(user, 1000);
        topUp(user, first, 1000, true);

        doPost(user, "/api/wallets/" + second + "/topup", body("amount", 1, "from_available", true))
                .andExpect(status().isBadRequest());

        assertMoney(walletBalance(user, second), "0");
        assertMoney(totalBalance(user), "1000");
    }

    @Test
    void topUpWithNewMoneyGrowsWalletAndTotal() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 0);

        doPost(user, "/api/wallets/" + wallet + "/topup", body("amount", 250, "from_available", false))
                .andExpect(status().isOk());

        assertMoney(walletBalance(user, wallet), "250");
        assertMoney(totalBalance(user), "250");
        assertMoney(unbudgeted(user), "0");
    }

    // ------------------------------------------------------------------ wallet create / edit (rule 5)

    @Test
    void newWalletAlwaysStartsEmptyEvenIfBalanceIsSent() throws Exception {
        TestUser user = register(PERSONAL);

        JsonNode created = data(doPost(user, "/api/wallets",
                body("name", "Travel", "category", "Travel", "balance", 5000, "budget_limit", 3000))
                .andExpect(status().isCreated()));

        assertMoney(created.get("balance"), "0");
        assertMoney(walletBalance(user, created.get("id").asText()), "0");
        assertMoney(totalBalance(user), "0");
        assertThat(wallets(user).size()).isEqualTo(5);
    }

    @Test
    void editingWalletChangesDetailsButNeverBalance() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 400, true);

        JsonNode updated = data(doPut(user, "/api/wallets/" + wallet,
                body("name", "Weekly Groceries", "budget_limit", 9000, "is_tax_reserve", true, "balance", 99999))
                .andExpect(status().isOk()));

        assertThat(updated.get("name").asText()).isEqualTo("Weekly Groceries");
        assertMoney(updated.get("budget_limit"), "9000");
        assertThat(updated.get("is_tax_reserve").asBoolean()).isTrue();
        assertMoney(updated.get("balance"), "400");

        JsonNode reloaded = getData(user, "/api/wallets/" + wallet);
        assertThat(reloaded.get("name").asText()).isEqualTo("Weekly Groceries");
        assertMoney(reloaded.get("balance"), "400");
        assertMoney(totalBalance(user), "1000");
    }

    // ------------------------------------------------------------------ savings goals (rule 6)

    @Test
    void newGoalAlwaysStartsAtZeroEvenIfAmountIsSent() throws Exception {
        TestUser user = register(STUDENT);
        addIncome(user, 1000);

        JsonNode goal = data(doPost(user, "/api/savings-goals",
                body("title", "Laptop", "target_amount", 50000, "current_amount", 20000))
                .andExpect(status().isCreated()));

        assertMoney(goal.get("current_amount"), "0");
        assertMoney(getData(user, "/api/savings-goals/" + goal.get("id").asText()).get("current_amount"), "0");
        assertMoney(totalBalance(user), "1000");
    }

    @Test
    void depositTakesFromUnbudgetedMoneyAndLowersTotal() throws Exception {
        TestUser user = register(STUDENT);
        addIncome(user, 1000);
        String goalId = createGoal(user, "Bike", false);

        JsonNode goal = data(doPost(user, "/api/savings-goals/" + goalId + "/deposit", body("amount", 300))
                .andExpect(status().isOk()));

        assertMoney(goal.get("current_amount"), "300");
        assertMoney(totalBalance(user), "700");
        assertMoney(unbudgeted(user), "700");
    }

    @Test
    void depositIsRejectedWhenNotEnoughUnbudgetedMoney() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 900, true); // only 100 left unbudgeted
        String goalId = createGoal(user, "Bike", false);

        doPost(user, "/api/savings-goals/" + goalId + "/deposit", body("amount", 200))
                .andExpect(status().isBadRequest());

        assertMoney(getData(user, "/api/savings-goals/" + goalId).get("current_amount"), "0");
        assertMoney(totalBalance(user), "1000");
        assertMoney(walletBalance(user, wallet), "900");
    }

    @Test
    void editingGoalCannotChangeSavedAmount() throws Exception {
        TestUser user = register(STUDENT);
        addIncome(user, 1000);
        String goalId = createGoal(user, "Bike", false);
        doPost(user, "/api/savings-goals/" + goalId + "/deposit", body("amount", 300)).andExpect(status().isOk());

        JsonNode updated = data(doPut(user, "/api/savings-goals/" + goalId,
                body("title", "Scooter", "target_amount", 8000, "current_amount", 99999))
                .andExpect(status().isOk()));

        assertThat(updated.get("title").asText()).isEqualTo("Scooter");
        assertMoney(updated.get("target_amount"), "8000");
        assertMoney(updated.get("current_amount"), "300");
        assertMoney(getData(user, "/api/savings-goals/" + goalId).get("current_amount"), "300");
        assertMoney(totalBalance(user), "700");
    }

    @Test
    void deletingGoalReturnsSavedMoneyToTotal() throws Exception {
        TestUser user = register(STUDENT);
        addIncome(user, 1000);
        String goalId = createGoal(user, "Trip", false);
        doPost(user, "/api/savings-goals/" + goalId + "/deposit", body("amount", 400)).andExpect(status().isOk());
        assertMoney(totalBalance(user), "600");

        doDelete(user, "/api/savings-goals/" + goalId).andExpect(status().isOk());

        doGet(user, "/api/savings-goals/" + goalId).andExpect(status().isNotFound());
        assertMoney(totalBalance(user), "1000");
        assertMoney(unbudgeted(user), "1000");
    }

    @Test
    void onlyOneGoalCanBeTheEmergencyFund() throws Exception {
        TestUser user = register(STUDENT);
        String first = createGoal(user, "Rainy day", true);
        String second = createGoal(user, "Medical", true);

        assertThat(isEmergency(user, first)).as("first goal cleared when second is created as emergency").isFalse();
        assertThat(isEmergency(user, second)).isTrue();

        doPut(user, "/api/savings-goals/" + first, body("is_emergency", true)).andExpect(status().isOk());

        assertThat(isEmergency(user, first)).isTrue();
        assertThat(isEmergency(user, second)).as("second goal cleared when first is edited to emergency").isFalse();
        long emergencyCount = 0;
        for (JsonNode g : getData(user, "/api/savings-goals")) {
            if (g.get("is_emergency").asBoolean()) emergencyCount++;
        }
        assertThat(emergencyCount).isEqualTo(1);
    }

    @Test
    void emergencyFlagOfOneUserDoesNotAffectAnother() throws Exception {
        TestUser alice = register(STUDENT);
        TestUser bob = register(STUDENT);
        String aliceGoal = createGoal(alice, "Alice fund", true);
        createGoal(bob, "Bob fund", true);

        assertThat(isEmergency(alice, aliceGoal)).isTrue();
    }

    // ------------------------------------------------------------------ helpers

    private String createGoal(TestUser user, String title, boolean emergency) throws Exception {
        return data(doPost(user, "/api/savings-goals",
                body("title", title, "target_amount", 10000, "is_emergency", emergency))
                .andExpect(status().isCreated())).get("id").asText();
    }

    private boolean isEmergency(TestUser user, String goalId) throws Exception {
        return getData(user, "/api/savings-goals/" + goalId).get("is_emergency").asBoolean();
    }
}
