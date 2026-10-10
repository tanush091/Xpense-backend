package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rule 8: recurring bills on the Personal dashboard. */
@SpringBootTest
@AutoConfigureMockMvc
class RecurringBillTest extends ApiTestSupport {

    private static final LocalDate DUE = LocalDate.now().plusDays(3);

    // ------------------------------------------------------------------ create

    @Test
    void createBillReturns201WithItsDetails() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 1);

        JsonNode bill = data(doPost(user, "/api/bills", body(
                "name", "Rent", "amount", 15000, "frequency", "monthly",
                "next_due_date", DUE.toString(), "wallet_id", wallet))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true)));

        assertThat(bill.get("id").asText()).isNotBlank();
        assertThat(bill.get("name").asText()).isEqualTo("Rent");
        assertMoney(bill.get("amount"), "15000");
        assertThat(bill.get("frequency").asText()).isEqualTo("monthly");
        assertThat(bill.get("next_due_date").asText()).isEqualTo(DUE.toString());
        assertThat(bill.get("wallet_id").asText()).isEqualTo(wallet);
        assertThat(bill.get("is_active").asBoolean()).isTrue();
    }

    static Stream<Arguments> invalidBills() {
        String due = DUE.toString();
        return Stream.of(
                Arguments.of("missing name", body("amount", 500, "frequency", "monthly", "next_due_date", due)),
                Arguments.of("blank name", body("name", "  ", "amount", 500, "frequency", "monthly", "next_due_date", due)),
                Arguments.of("missing amount", body("name", "Wifi", "frequency", "monthly", "next_due_date", due)),
                Arguments.of("zero amount", body("name", "Wifi", "amount", 0, "frequency", "monthly", "next_due_date", due)),
                Arguments.of("missing next_due_date", body("name", "Wifi", "amount", 500, "frequency", "monthly")),
                Arguments.of("daily frequency", body("name", "Wifi", "amount", 500, "frequency", "daily", "next_due_date", due)));
    }

    @ParameterizedTest(name = "{0} -> 400")
    @MethodSource("invalidBills")
    void invalidBillIsRejected(String why, Map<String, Object> bill) throws Exception {
        TestUser user = register(PERSONAL);

        doPost(user, "/api/bills", bill)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(getData(user, "/api/bills").size()).isZero();
    }

    @Test
    void billCannotUseAnotherUsersWallet() throws Exception {
        TestUser alice = register(PERSONAL);
        TestUser bob = register(PERSONAL);
        String aliceWallet = walletId(alice, 0);

        doPost(bob, "/api/bills", body("name", "Gym", "amount", 999, "frequency", "monthly",
                "next_due_date", DUE.toString(), "wallet_id", aliceWallet))
                .andExpect(status().isNotFound());

        assertThat(getData(bob, "/api/bills").size()).isZero();
    }

    // ------------------------------------------------------------------ list

    @Test
    void listShowsOnlyMyActiveBillsSoonestFirst() throws Exception {
        TestUser alice = register(PERSONAL);
        TestUser bob = register(PERSONAL);
        createBill(alice, "Insurance", 2000, "yearly", LocalDate.now().plusDays(20), null);
        createBill(alice, "Phone", 500, "monthly", LocalDate.now().plusDays(5), null);
        createBill(alice, "Netflix", 650, "monthly", LocalDate.now().plusDays(12), null);
        String cancelled = createBill(alice, "Old gym", 900, "monthly", LocalDate.now().plusDays(1), null);
        createBill(bob, "Bob's rent", 9000, "monthly", LocalDate.now().plusDays(2), null);
        doDelete(alice, "/api/bills/" + cancelled).andExpect(status().isOk());

        assertThat(texts(getData(alice, "/api/bills"), "name")).containsExactly("Phone", "Netflix", "Insurance");
        assertThat(texts(getData(bob, "/api/bills"), "name")).containsExactly("Bob's rent");
    }

    // ------------------------------------------------------------------ pay

    @Test
    void payingBillRecordsExpenseAndMovesDueDateForward() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 1);
        addIncome(user, 20000);
        topUp(user, wallet, 16000, true);
        String billId = createBill(user, "Rent", 15000, "monthly", DUE, wallet);

        JsonNode result = data(doPost(user, "/api/bills/" + billId + "/pay", null).andExpect(status().isOk()));

        JsonNode bill = result.get("bill");
        assertThat(bill.get("id").asText()).isEqualTo(billId);
        assertThat(bill.get("last_paid_date").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(bill.get("next_due_date").asText()).isEqualTo(DUE.plusMonths(1).toString());

        JsonNode tx = result.get("transaction");
        assertThat(tx.get("title").asText()).isEqualTo("Rent");
        assertThat(tx.get("type").asText()).isEqualTo("expense");
        assertThat(tx.get("wallet_id").asText()).isEqualTo(wallet);
        assertMoney(tx.get("amount"), "15000");

        assertThat(texts(getData(user, "/api/transactions"), "id")).contains(tx.get("id").asText());
        assertMoney(walletBalance(user, wallet), "1000");
        assertMoney(totalBalance(user), "5000");

        JsonNode listed = getData(user, "/api/bills").get(0);
        assertThat(listed.get("next_due_date").asText()).isEqualTo(DUE.plusMonths(1).toString());
    }

    @ParameterizedTest(name = "{0} bill: next due moves by one period")
    @CsvSource({"weekly", "monthly", "yearly"})
    void dueDateAdvancesByOnePeriod(String frequency) throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 1000, true);
        String billId = createBill(user, "Subscription", 100, frequency, DUE, wallet);

        JsonNode bill = data(doPost(user, "/api/bills/" + billId + "/pay", null).andExpect(status().isOk())).get("bill");

        LocalDate expected = switch (frequency) {
            case "weekly" -> DUE.plusDays(7);
            case "yearly" -> DUE.plusYears(1);
            default -> DUE.plusMonths(1);
        };
        assertThat(bill.get("next_due_date").asText()).isEqualTo(expected.toString());
        assertThat(bill.get("frequency").asText()).isEqualTo(frequency);
    }

    @Test
    void payingTwiceAdvancesTwice() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 1000, true);
        String billId = createBill(user, "Milk", 100, "weekly", DUE, wallet);

        doPost(user, "/api/bills/" + billId + "/pay", null).andExpect(status().isOk());
        JsonNode bill = data(doPost(user, "/api/bills/" + billId + "/pay", null).andExpect(status().isOk())).get("bill");

        assertThat(bill.get("next_due_date").asText()).isEqualTo(DUE.plusDays(14).toString());
        assertMoney(walletBalance(user, wallet), "800");
        assertThat(transactionCount(user)).isEqualTo(3);
    }

    @Test
    void payingWithoutEnoughMoneyChangesNothing() throws Exception {
        TestUser user = register(PERSONAL);
        String wallet = walletId(user, 0);
        addIncome(user, 1000);
        topUp(user, wallet, 500, true);
        String billId = createBill(user, "Electricity", 800, "monthly", DUE, wallet);
        int txBefore = transactionCount(user);

        doPost(user, "/api/bills/" + billId + "/pay", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(transactionCount(user)).as("no expense recorded").isEqualTo(txBefore);
        assertMoney(walletBalance(user, wallet), "500");
        assertMoney(totalBalance(user), "1000");
        JsonNode bill = getData(user, "/api/bills").get(0);
        assertThat(bill.get("id").asText()).isEqualTo(billId);
        assertThat(bill.get("next_due_date").asText()).isEqualTo(DUE.toString());
        assertThat(bill.path("last_paid_date").isNull() || bill.path("last_paid_date").isMissingNode())
                .as("last_paid_date not set").isTrue();
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deletedBillIsHiddenAndCannotBePaid() throws Exception {
        TestUser user = register(PERSONAL);
        addIncome(user, 5000);
        String billId = createBill(user, "Magazine", 200, "monthly", DUE, null);

        doDelete(user, "/api/bills/" + billId).andExpect(status().isOk());

        assertThat(texts(getData(user, "/api/bills"), "id")).doesNotContain(billId);
        doPost(user, "/api/bills/" + billId + "/pay", null).andExpect(status().isNotFound());
        assertThat(transactionCount(user)).isEqualTo(1);
        assertMoney(totalBalance(user), "5000");
    }

    // ------------------------------------------------------------------ helpers

    private String createBill(TestUser user, String name, int amount, String frequency, LocalDate due, String walletId)
            throws Exception {
        return data(doPost(user, "/api/bills", body("name", name, "amount", amount, "frequency", frequency,
                "next_due_date", due.toString(), "wallet_id", walletId))
                .andExpect(status().isCreated())).get("id").asText();
    }
}
