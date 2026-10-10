package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rule 9: business analytics (money in vs out per month, top payees), plus the Corporate tax-reserve budget. */
@SpringBootTest
@AutoConfigureMockMvc
class BusinessAnalyticsTest extends ApiTestSupport {

    @Test
    void corporateAccountGetsOneTaxReserveBudget() throws Exception {
        TestUser user = register(CORPORATE);

        JsonNode wallets = wallets(user);
        assertThat(wallets.size()).isEqualTo(4);
        int reserves = 0;
        for (JsonNode w : wallets) {
            assertMoney(w.get("balance"), "0");
            if (w.get("is_tax_reserve").asBoolean()) {
                reserves++;
                assertThat(w.get("name").asText()).isEqualTo("Tax & Contingency Reserve");
            }
        }
        assertThat(reserves).isEqualTo(1);
    }

    // ------------------------------------------------------------------ monthly

    @Test
    void monthlyReturnsSixMonthsOldestFirstEndingThisMonth() throws Exception {
        TestUser user = register(CORPORATE);

        JsonNode months = getData(user, "/api/analytics/monthly?months=6");

        assertThat(months.isArray()).isTrue();
        assertThat(months.size()).isEqualTo(6);
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 6; i++) {
            JsonNode m = months.get(i);
            assertThat(m.get("month").asText()).isEqualTo(now.minusMonths(5 - i).toString());
            assertThat(m.get("label").asText()).isNotBlank();
            assertMoney(m.get("money_in"), "0");
            assertMoney(m.get("money_out"), "0");
        }
    }

    @Test
    void monthlyTotalsMatchThisMonthsTransactions() throws Exception {
        TestUser user = register(CORPORATE);
        String wallet = walletId(user, 0);
        addIncome(user, 10000);
        addIncome(user, 2500);
        topUp(user, wallet, 4000, true); // moving money into a budget is not money in or out
        addExpense(user, wallet, 1500, "Hosting");
        addExpense(user, null, 500, "Courier");

        JsonNode months = getData(user, "/api/analytics/monthly?months=6");

        JsonNode current = months.get(5);
        assertThat(current.get("month").asText()).isEqualTo(YearMonth.now().toString());
        assertMoney(current.get("money_in"), "12500");
        assertMoney(current.get("money_out"), "2000");
        for (int i = 0; i < 5; i++) {
            assertMoney(months.get(i).get("money_in"), "0");
            assertMoney(months.get(i).get("money_out"), "0");
        }
    }

    @Test
    void monthlyDefaultsToSixMonths() throws Exception {
        TestUser user = register(CORPORATE);

        assertThat(getData(user, "/api/analytics/monthly").size()).isEqualTo(6);
    }

    @ParameterizedTest(name = "months={0} -> {1} rows")
    @CsvSource({"0, 1", "-3, 1", "1, 1", "12, 12", "24, 24", "25, 24", "100, 24"})
    void monthsParameterIsClampedBetweenOneAndTwentyFour(int requested, int expected) throws Exception {
        TestUser user = register(CORPORATE);

        JsonNode months = getData(user, "/api/analytics/monthly?months=" + requested);

        assertThat(months.size()).isEqualTo(expected);
        assertThat(months.get(expected - 1).get("month").asText()).isEqualTo(YearMonth.now().toString());
    }

    @Test
    void monthlyOnlyCountsMyOwnTransactions() throws Exception {
        TestUser alice = register(CORPORATE);
        TestUser bob = register(CORPORATE);
        addIncome(alice, 7000);
        addExpense(alice, null, 1000, "Supplies");

        JsonNode bobCurrent = getData(bob, "/api/analytics/monthly?months=1").get(0);

        assertMoney(bobCurrent.get("money_in"), "0");
        assertMoney(bobCurrent.get("money_out"), "0");
    }

    // ------------------------------------------------------------------ payees

    @Test
    void payeesAreGroupedByMerchantThenRecipientThenTitleBiggestFirst() throws Exception {
        TestUser user = register(CORPORATE);
        addIncome(user, 20000);
        spend(user, "Server bill", 3000, "AWS", null);
        spend(user, "Storage", 1000, "AWS", null);
        spend(user, "Design work", 1500, null, "Priya");
        spend(user, "Office Rent", 4500, null, null);

        JsonNode payees = getData(user, "/api/analytics/payees?limit=5");

        assertThat(texts(payees, "name")).containsExactly("Office Rent", "AWS", "Priya");
        assertPayee(payees.get(0), "4500", 45, 1);
        assertPayee(payees.get(1), "4000", 40, 2);
        assertPayee(payees.get(2), "1500", 15, 1);
    }

    @Test
    void payeesIgnoreMoneyInAndRespectLimit() throws Exception {
        TestUser user = register(CORPORATE);
        addIncome(user, 20000);
        spend(user, "A", 300, "Vendor A", null);
        spend(user, "B", 200, "Vendor B", null);
        spend(user, "C", 100, "Vendor C", null);

        JsonNode top2 = getData(user, "/api/analytics/payees?limit=2");

        assertThat(texts(top2, "name")).containsExactly("Vendor A", "Vendor B");
        assertThat(texts(getData(user, "/api/analytics/payees"), "name")).doesNotContain("Income");
    }

    @Test
    void payeesOnlyIncludeThisMonthsSpending() throws Exception {
        TestUser user = register(CORPORATE);
        addIncome(user, 20000);
        spend(user, "Laptop", 2000, "Croma", null);
        String lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(15).atTime(10, 0).toString();
        doPost(user, "/api/transactions", body("title", "Old order", "amount", 900, "type", "expense",
                "category", "General", "merchant", "Old Vendor", "date", lastMonth))
                .andExpect(status().isCreated());

        JsonNode payees = getData(user, "/api/analytics/payees");

        assertThat(texts(payees, "name")).containsExactly("Croma");
        assertPayee(payees.get(0), "2000", 100, 1);
        JsonNode months = getData(user, "/api/analytics/monthly?months=2");
        assertMoney(months.get(0).get("money_out"), "900");
        assertMoney(months.get(1).get("money_out"), "2000");
    }

    @Test
    void payeesWithNoSpendingIsEmpty() throws Exception {
        TestUser user = register(CORPORATE);

        assertThat(getData(user, "/api/analytics/payees").size()).isZero();
    }

    @Test
    void payeesRejectBadDates() throws Exception {
        TestUser user = register(CORPORATE);

        doGet(user, "/api/analytics/payees?from=abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        doGet(user, "/api/analytics/payees?to=2026-13-45").andExpect(status().isBadRequest());
        doGet(user, "/api/analytics/payees?from=2026-05-10&to=2026-05-01").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ helpers

    private void spend(TestUser user, String title, int amount, String merchant, String recipient) throws Exception {
        doPost(user, "/api/transactions", body("title", title, "amount", amount, "type", "expense",
                "category", "General", "merchant", merchant, "recipient", recipient))
                .andExpect(status().isCreated());
    }

    private static void assertPayee(JsonNode payee, String amount, int percent, int payments) {
        assertMoney(payee.get("amount"), amount);
        assertThat(payee.get("percent").asInt()).as("percent of %s", payee.get("name").asText()).isEqualTo(percent);
        assertThat(payee.get("payments").asInt()).as("payments of %s", payee.get("name").asText()).isEqualTo(payments);
    }
}
