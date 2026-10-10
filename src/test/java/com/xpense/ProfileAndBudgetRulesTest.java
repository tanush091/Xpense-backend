package com.xpense;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rule 7 (profile edits) and rule 10 (category spending limits are one per category). */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileAndBudgetRulesTest extends ApiTestSupport {

    // ------------------------------------------------------------------ profile (rule 7)

    @Test
    void profileEditChangesNameBusinessNameAndTaxPercent() throws Exception {
        TestUser user = register(CORPORATE);

        doPut(user, "/api/profile", body("full_name", "Meera Iyer", "business_name", "Acme Labs", "tax_reserve_percent", 25))
                .andExpect(status().isOk());

        JsonNode profile = getData(user, "/api/profile");
        assertThat(profile.get("full_name").asText()).isEqualTo("Meera Iyer");
        assertThat(profile.get("business_name").asText()).isEqualTo("Acme Labs");
        assertMoney(profile.get("tax_reserve_percent"), "25");
    }

    @ParameterizedTest(name = "tax_reserve_percent {0} -> 400")
    @ValueSource(strings = {"61", "-1", "60.01", "100"})
    void taxPercentOutsideZeroToSixtyIsRejected(String percent) throws Exception {
        TestUser user = register(CORPORATE);
        doPut(user, "/api/profile", body("tax_reserve_percent", 20)).andExpect(status().isOk());

        doPut(user, "/api/profile", body("tax_reserve_percent", new BigDecimal(percent)))
                .andExpect(status().isBadRequest());

        assertMoney(getData(user, "/api/profile").get("tax_reserve_percent"), "20");
    }

    @ParameterizedTest(name = "tax_reserve_percent {0} is allowed")
    @ValueSource(strings = {"0", "60"})
    void taxPercentBoundariesAreAllowed(String percent) throws Exception {
        TestUser user = register(CORPORATE);

        doPut(user, "/api/profile", body("tax_reserve_percent", new BigDecimal(percent)))
                .andExpect(status().isOk());

        assertMoney(getData(user, "/api/profile").get("tax_reserve_percent"), percent);
    }

    @Test
    void profileEditCannotChangeBalanceEmailRoleOrAccountType() throws Exception {
        TestUser user = register(PERSONAL);
        addIncome(user, 1000);

        doPut(user, "/api/profile", body(
                "full_name", "Still Me",
                "total_balance", 999999,
                "totalBalance", 999999,
                "email", "attacker@example.com",
                "role", "admin",
                "account_type", "Corporate SaaS"))
                .andExpect(status().isOk());

        JsonNode profile = getData(user, "/api/profile");
        assertThat(profile.get("full_name").asText()).isEqualTo("Still Me");
        assertMoney(profile.get("total_balance"), "1000");
        assertThat(profile.get("email").asText()).isEqualTo(user.email());
        assertThat(profile.get("role").asText()).isEqualTo("user");
        assertThat(profile.get("account_type").asText()).isEqualTo(PERSONAL);

        // The original email still signs in
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body("email", user.email(), "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ category limits (rule 10)

    @Test
    void postingSameCategoryTwiceUpdatesInsteadOfDuplicating() throws Exception {
        TestUser user = register(STUDENT);

        String firstId = data(doPost(user, "/api/budgets", body("category", "Food", "limit_amount", 3000))
                .andExpect(status().isCreated())).get("id").asText();
        JsonNode second = data(doPost(user, "/api/budgets", body("category", "Food", "limit_amount", 4500))
                .andExpect(status().isCreated()));

        assertThat(second.get("id").asText()).isEqualTo(firstId);
        JsonNode all = getData(user, "/api/budgets");
        assertThat(all.size()).isEqualTo(1);
        assertThat(all.get(0).get("category").asText()).isEqualTo("Food");
        assertMoney(all.get(0).get("limit_amount"), "4500");
    }

    @Test
    void differentCategoriesCreateSeparateLimits() throws Exception {
        TestUser user = register(STUDENT);

        doPost(user, "/api/budgets", body("category", "Food", "limit_amount", 3000)).andExpect(status().isCreated());
        doPost(user, "/api/budgets", body("category", "Travel", "limit_amount", 1000)).andExpect(status().isCreated());

        assertThat(texts(getData(user, "/api/budgets"), "category")).containsExactlyInAnyOrder("Food", "Travel");
    }

    @Test
    void sameCategoryForAnotherUserDoesNotTouchMyLimit() throws Exception {
        TestUser alice = register(STUDENT);
        TestUser bob = register(STUDENT);

        String aliceId = data(doPost(alice, "/api/budgets", body("category", "Food", "limit_amount", 3000))
                .andExpect(status().isCreated())).get("id").asText();
        String bobId = data(doPost(bob, "/api/budgets", body("category", "Food", "limit_amount", 1000))
                .andExpect(status().isCreated())).get("id").asText();

        assertThat(bobId).isNotEqualTo(aliceId);
        assertMoney(getData(alice, "/api/budgets/" + aliceId).get("limit_amount"), "3000");
        assertThat(getData(bob, "/api/budgets").size()).isEqualTo(1);
    }

    @Test
    void categoryLimitNeedsCategoryAndPositiveAmount() throws Exception {
        TestUser user = register(STUDENT);

        doPost(user, "/api/budgets", body("limit_amount", 3000)).andExpect(status().isBadRequest());
        doPost(user, "/api/budgets", body("category", "Food", "limit_amount", 0)).andExpect(status().isBadRequest());

        assertThat(getData(user, "/api/budgets").size()).isZero();
    }
}
