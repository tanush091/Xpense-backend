package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixes found by API testing: simultaneous money requests, partial edits wiping fields,
 * month-end bill dates, history for new money, one tax budget, and account-type checks.
 * Runs against a real server port so requests truly overlap.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyAndPartialUpdateTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    private final HttpClient http = HttpClient.newHttpClient();

    private record Res(int status, JsonNode body) {
    }

    private Res call(String method, String path, Object body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api" + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body().isEmpty() ? null : mapper.readTree(r.body()));
    }

    private String register(String accountType) throws Exception {
        Res r = call("POST", "/auth/register", Map.of(
                "email", "conc_" + System.nanoTime() + "@test.com",
                "password", "password123",
                "fullName", "Race Tester",
                "accountType", accountType), null);
        assertThat(r.status()).isEqualTo(201);
        return r.body().get("data").get("token").asText();
    }

    private JsonNode firstWallet(String token) throws Exception {
        return call("GET", "/wallets", null, token).body().get("data").get(0);
    }

    private double total(String token) throws Exception {
        return call("GET", "/profile", null, token).body().get("data").get("total_balance").asDouble();
    }

    private double walletBalance(String token, String walletId) throws Exception {
        return call("GET", "/wallets/" + walletId, null, token).body().get("data").get("balance").asDouble();
    }

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = register("Personal Account");
    }

    @Test
    void simultaneousExpensesCannotSpendTheSameMoneyTwice() throws Exception {
        String walletId = firstWallet(token).get("id").asText();
        call("POST", "/transactions", Map.of("title", "Salary", "amount", 1000, "type", "income"), token);
        call("POST", "/wallets/" + walletId + "/topup", Map.of("amount", 1000, "from_available", true), token);

        int requests = 8;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return call("POST", "/transactions", Map.of(
                        "title", "race", "amount", 600, "type", "expense", "wallet_id", walletId), token).status();
            }));
        }
        start.countDown();
        int created = 0;
        for (Future<Integer> f : results) {
            if (f.get() == 201) created++;
        }
        pool.shutdown();

        assertThat(created).isEqualTo(1);
        assertThat(walletBalance(token, walletId)).isEqualTo(400.0);
        assertThat(total(token)).isEqualTo(400.0);
        long raceRows = 0;
        for (JsonNode tx : call("GET", "/transactions", null, token).body().get("data")) {
            if ("race".equals(tx.get("title").asText())) raceRows++;
        }
        assertThat(raceRows).isEqualTo(1);
    }

    @Test
    void renamingABudgetKeepsItsOtherSettings() throws Exception {
        String biz = register("Corporate SaaS");
        JsonNode tax = null;
        for (JsonNode w : call("GET", "/wallets", null, biz).body().get("data")) {
            if (w.get("is_tax_reserve").asBoolean()) tax = w;
        }
        assertThat(tax).isNotNull();
        Res r = call("PUT", "/wallets/" + tax.get("id").asText(), Map.of("name", "Tax pot"), biz);
        assertThat(r.status()).isEqualTo(200);
        JsonNode after = r.body().get("data");
        assertThat(after.get("name").asText()).isEqualTo("Tax pot");
        assertThat(after.get("is_tax_reserve").asBoolean()).isTrue();
        assertThat(after.get("budget_limit").asDouble()).isEqualTo(tax.get("budget_limit").asDouble());
        assertThat(after.get("icon").asText()).isEqualTo(tax.get("icon").asText());
    }

    @Test
    void savingOnlyANameKeepsTheTaxRate() throws Exception {
        String biz = register("Corporate SaaS");
        call("PUT", "/profile", Map.of("tax_reserve_percent", 18), biz);
        Res r = call("PUT", "/profile", Map.of("full_name", "New Name"), biz);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("data").get("tax_reserve_percent").asDouble()).isEqualTo(18.0);
        assertThat(r.body().get("data").get("university").asText()).isEmpty();
    }

    @Test
    void renamingAGoalKeepsTheEmergencyFlag() throws Exception {
        Res g = call("POST", "/savings-goals", Map.of("title", "Emergency", "target_amount", 5000, "is_emergency", true), token);
        String id = g.body().get("data").get("id").asText();
        Res r = call("PUT", "/savings-goals/" + id, Map.of("title", "Rainy day"), token);
        assertThat(r.body().get("data").get("is_emergency").asBoolean()).isTrue();
    }

    @Test
    void billsDueOnThe31stStayAtMonthEnd() throws Exception {
        String walletId = firstWallet(token).get("id").asText();
        call("POST", "/transactions", Map.of("title", "Salary", "amount", 3000, "type", "income"), token);
        call("POST", "/wallets/" + walletId + "/topup", Map.of("amount", 3000, "from_available", true), token);
        Res bill = call("POST", "/bills", Map.of("name", "Rent", "amount", 1000, "frequency", "monthly",
                "next_due_date", "2027-01-31", "wallet_id", walletId), token);
        String id = bill.body().get("data").get("id").asText();

        Res first = call("POST", "/bills/" + id + "/pay", null, token);
        assertThat(first.body().get("data").get("bill").get("next_due_date").asText()).isEqualTo("2027-02-28");
        Res second = call("POST", "/bills/" + id + "/pay", null, token);
        assertThat(second.body().get("data").get("bill").get("next_due_date").asText()).isEqualTo("2027-03-31");
    }

    @Test
    void newMoneyIntoABudgetIsRecordedAndCanBeUndone() throws Exception {
        String walletId = firstWallet(token).get("id").asText();
        call("POST", "/wallets/" + walletId + "/topup", Map.of("amount", 500, "from_available", false), token);

        JsonNode moneyIn = null;
        for (JsonNode tx : call("GET", "/transactions", null, token).body().get("data")) {
            if ("income".equals(tx.get("type").asText())) moneyIn = tx;
        }
        assertThat(moneyIn).isNotNull();
        assertThat(moneyIn.get("amount").asDouble()).isEqualTo(500.0);
        assertThat(total(token)).isEqualTo(500.0);

        Res del = call("DELETE", "/transactions/" + moneyIn.get("id").asText(), null, token);
        assertThat(del.status()).isEqualTo(200);
        assertThat(walletBalance(token, walletId)).isEqualTo(0.0);
        assertThat(total(token)).isEqualTo(0.0);
    }

    @Test
    void topUpWithoutSayingWhereMovesExistingMoney() throws Exception {
        String walletId = firstWallet(token).get("id").asText();
        Res r = call("POST", "/wallets/" + walletId + "/topup", Map.of("amount", 100), token);
        assertThat(r.status()).isEqualTo(400);
        assertThat(total(token)).isEqualTo(0.0);
    }

    @Test
    void onlyOneTaxBudgetAtATime() throws Exception {
        String biz = register("Corporate SaaS");
        Res created = call("POST", "/wallets", Map.of("name", "New tax", "category", "Tax",
                "budget_limit", 5000, "is_tax_reserve", true), biz);
        assertThat(created.status()).isEqualTo(201);
        int taxBudgets = 0;
        for (JsonNode w : call("GET", "/wallets", null, biz).body().get("data")) {
            if (w.get("is_tax_reserve").asBoolean()) taxBudgets++;
        }
        assertThat(taxBudgets).isEqualTo(1);
    }

    @Test
    void unknownAccountTypeIsRejectedAndBadValuesAre400() throws Exception {
        Res r = call("POST", "/auth/register", Map.of("email", "gold_" + System.nanoTime() + "@test.com",
                "password", "password123", "fullName", "Gold", "accountType", "Gold Account"), null);
        assertThat(r.status()).isEqualTo(400);

        assertThat(call("POST", "/transactions", Map.of("title", "x".repeat(300), "amount", 10, "type", "income"), token).status())
                .isEqualTo(400);
        assertThat(call("POST", "/transactions", Map.of("title", "big", "amount", 1e20, "type", "income"), token).status())
                .isEqualTo(400);
        assertThat(call("POST", "/transactions", Map.of("title", "tiny", "amount", 0.001, "type", "income"), token).status())
                .isEqualTo(400);
        assertThat(call("GET", "/nope", null, token).status()).isEqualTo(404);
    }
}
