package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rule 3: how money in / money out moves the total balance, budgets and "not in a budget yet". */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionMoneyRulesTest extends ApiTestSupport {

    @Test
    void incomeRaisesTotalAndLandsUnbudgetedEvenIfWalletIsSent() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);

        JsonNode tx = data(doPost(user, "/api/transactions",
                body("title", "Pocket money", "amount", 1500, "type", "income", "category", "Allowance", "wallet_id", wallet))
                .andExpect(status().isCreated()));

        assertThat(tx.get("type").asText()).isEqualTo("income");
        assertThat(tx.path("wallet_id").isNull() || tx.path("wallet_id").isMissingNode())
                .as("income must not be linked to a wallet").isTrue();
        assertMoney(totalBalance(user), "1500");
        assertMoney(walletBalance(user, wallet), "0");
        assertMoney(unbudgeted(user), "1500");
    }

    @Test
    void expenseFromWalletLowersWalletAndTotal() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 600, true);

        JsonNode tx = data(doPost(user, "/api/transactions",
                body("title", "Lunch", "amount", 250, "type", "expense", "category", "Food & Dining", "wallet_id", wallet))
                .andExpect(status().isCreated()));

        assertThat(tx.get("wallet_id").asText()).isEqualTo(wallet);
        assertMoney(walletBalance(user, wallet), "350");
        assertMoney(totalBalance(user), "750");
        assertMoney(unbudgeted(user), "400");
    }

    @Test
    void expenseBiggerThanWalletBalanceIsRejectedAndChangesNothing() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 300, true);

        doPost(user, "/api/transactions",
                body("title", "Too much", "amount", 400, "type", "expense", "category", "General", "wallet_id", wallet))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertMoney(walletBalance(user, wallet), "300");
        assertMoney(totalBalance(user), "1000");
        assertThat(transactionCount(user)).isEqualTo(1);
    }

    @Test
    void expenseWithoutWalletOnlyUsesUnbudgetedMoney() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 800, true); // 200 left unbudgeted

        doPost(user, "/api/transactions", body("title", "Cab", "amount", 300, "type", "expense", "category", "General"))
                .andExpect(status().isBadRequest());
        assertMoney(totalBalance(user), "1000");
        assertMoney(walletBalance(user, wallet), "800");

        addExpense(user, null, 200, "Cab");
        assertMoney(totalBalance(user), "800");
        assertMoney(walletBalance(user, wallet), "800");
        assertMoney(unbudgeted(user), "0");
    }

    @Test
    void expenseWithoutWalletIsRejectedForBrandNewUserWithNoMoney() throws Exception {
        TestUser user = register(STUDENT);

        doPost(user, "/api/transactions", body("title", "Snack", "amount", 50, "type", "expense", "category", "General"))
                .andExpect(status().isBadRequest());

        assertMoney(totalBalance(user), "0");
        assertThat(transactionCount(user)).isZero();
    }

    @ParameterizedTest(name = "amount {0} -> 400")
    @ValueSource(strings = {"0", "-50", "-0.01"})
    void nonPositiveAmountIsRejected(String amount) throws Exception {
        TestUser user = register(STUDENT);
        addIncome(user, 1000);

        doPost(user, "/api/transactions", body("title", "Bad", "amount", new BigDecimal(amount), "type", "income"))
                .andExpect(status().isBadRequest());
        doPost(user, "/api/transactions", body("title", "Bad", "amount", new BigDecimal(amount), "type", "expense"))
                .andExpect(status().isBadRequest());

        assertMoney(totalBalance(user), "1000");
        assertThat(transactionCount(user)).isEqualTo(1);
    }

    @Test
    void missingAmountIsRejected() throws Exception {
        TestUser user = register(STUDENT);

        doPost(user, "/api/transactions", body("title", "No amount", "type", "income"))
                .andExpect(status().isBadRequest());

        assertMoney(totalBalance(user), "0");
    }

    @ParameterizedTest(name = "type \"{0}\" -> 400")
    @ValueSource(strings = {"refund", "salary", "deposit", ""})
    void unknownTypeIsRejected(String type) throws Exception {
        TestUser user = register(STUDENT);

        doPost(user, "/api/transactions", body("title", "Odd", "amount", 100, "type", type))
                .andExpect(status().isBadRequest());

        assertMoney(totalBalance(user), "0");
        assertThat(transactionCount(user)).isZero();
    }

    @Test
    void missingTypeIsRejected() throws Exception {
        TestUser user = register(STUDENT);

        doPost(user, "/api/transactions", body("title", "No type", "amount", 100))
                .andExpect(status().isBadRequest());

        assertMoney(totalBalance(user), "0");
    }

    @Test
    void transferTypeIsAcceptedAndCountsAsMoneyOut() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 500, true);

        doPost(user, "/api/transactions",
                body("title", "To Ravi", "amount", 100, "type", "transfer", "category", "Transfers", "wallet_id", wallet))
                .andExpect(status().isCreated());

        assertMoney(walletBalance(user, wallet), "400");
        assertMoney(totalBalance(user), "900");
    }

    @Test
    void deletingIncomeThatIsAlreadyBudgetedIsRejected() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        String incomeId = addIncome(user, 1000);
        topUp(user, wallet, 700, true); // only 300 left unbudgeted

        doDelete(user, "/api/transactions/" + incomeId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        doGet(user, "/api/transactions/" + incomeId).andExpect(status().isOk());
        assertMoney(totalBalance(user), "1000");
        assertMoney(walletBalance(user, wallet), "700");
    }

    @Test
    void deletingIncomeThatIsStillUnbudgetedReversesIt() throws Exception {
        TestUser user = register(STUDENT);
        addIncome(user, 1000);
        String secondIncome = addIncome(user, 500);

        doDelete(user, "/api/transactions/" + secondIncome).andExpect(status().isOk());

        doGet(user, "/api/transactions/" + secondIncome).andExpect(status().isNotFound());
        assertMoney(totalBalance(user), "1000");
    }

    @Test
    void deletingExpenseRefundsWalletAndTotal() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 600, true);
        String expenseId = addExpense(user, wallet, 200, "Books");
        assertMoney(walletBalance(user, wallet), "400");
        assertMoney(totalBalance(user), "800");

        doDelete(user, "/api/transactions/" + expenseId).andExpect(status().isOk());

        doGet(user, "/api/transactions/" + expenseId).andExpect(status().isNotFound());
        assertMoney(walletBalance(user, wallet), "600");
        assertMoney(totalBalance(user), "1000");
    }

    @Test
    void deletingUnbudgetedExpenseRefundsTotalOnly() throws Exception {
        TestUser user = register(STUDENT);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        String expenseId = addExpense(user, null, 300, "Cab");
        assertMoney(totalBalance(user), "700");

        doDelete(user, "/api/transactions/" + expenseId).andExpect(status().isOk());

        assertMoney(totalBalance(user), "1000");
        assertMoney(walletBalance(user, wallet), "0");
    }
}
