package com.finai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the write-to-AI path against a real database.
 *
 * The unit suite proves the analyst assembles a correct answer from a snapshot, but
 * it cannot catch a wiring fault between the controller, the persisted transactions
 * and the analyst. This is the regression guard for the SPENDING_ANALYSIS 500: the
 * question that used to raise UnsupportedOperationException now has to answer 200
 * and cite the figures that were actually written.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiGroundingIT extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long foodCategoryId;
    private Long salaryCategoryId;

    @BeforeEach
    void seedUserWithFinancialData() throws Exception {
        String stamp = UUID.randomUUID().toString().substring(0, 8);

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "ai." + stamp + "@example.com",
                                "username", "ai_" + stamp,
                                "password", "Password1"))))
                .andExpect(status().isCreated())
                .andReturn();
        token = json(registered).get("accessToken").asText();

        MvcResult categories = mockMvc.perform(get("/api/categories").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode category : json(categories)) {
            if ("FOOD".equals(category.get("code").asText())) {
                foodCategoryId = category.get("id").asLong();
            }
            if ("SALARY".equals(category.get("code").asText())) {
                salaryCategoryId = category.get("id").asLong();
            }
        }
        assertThat(foodCategoryId).isNotNull();
        assertThat(salaryCategoryId).isNotNull();

        createTransaction(salaryCategoryId, "INCOME", "15000000.00", "Monthly salary");
        createTransaction(foodCategoryId, "EXPENSE", "3600000.00", "Groceries");
        createTransaction(foodCategoryId, "EXPENSE", "1200000.00", "Eating out");
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void createTransaction(Long categoryId, String type, String amount, String note) throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId,
                                "type", type,
                                "amount", amount,
                                "note", note,
                                "transactionDate", LocalDate.now().toString()))))
                .andExpect(status().isCreated());
    }

    private JsonNode ask(String message) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", message))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result);
    }

    @Test
    @DisplayName("a top-category question answers 200 and cites the stored figures")
    void answersTopCategoryQuestion() throws Exception {
        JsonNode response = ask("This month, where did I spend the most?");

        assertThat(response.get("intent").asText()).isEqualTo("SPENDING_ANALYSIS");
        assertThat(response.get("grounded").asBoolean()).isTrue();
        assertThat(response.get("conversationId").asLong()).isPositive();

        String answer = response.get("answer").asText();
        assertThat(answer).contains("FACT").contains("SUGGESTION");
        assertThat(answer).contains("Food");
    }

    @Test
    @DisplayName("the total expense fact matches what was actually written")
    void reportsTheRealExpenseTotal() throws Exception {
        JsonNode response = ask("This month, where did I spend the most?");

        // 3,600,000 + 1,200,000.
        assertThat(response.get("facts").toString()).contains("totalExpense=4800000.00");
    }

    @Test
    @DisplayName("a question about a period with no data is answered, not refused with an error")
    void answersOverviewQuestion() throws Exception {
        JsonNode response = ask("How much is my balance this month?");

        assertThat(response.get("grounded").asBoolean()).isTrue();
        assertThat(response.get("answer").asText()).contains("15.000.000");
    }

    @Test
    @DisplayName("the conversation transcript is persisted and readable afterwards")
    void persistsTheTranscript() throws Exception {
        long conversationId = ask("This month, where did I spend the most?").get("conversationId").asLong();

        MvcResult transcript = mockMvc.perform(
                        get("/api/ai/conversations/" + conversationId + "/messages")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode messages = json(transcript);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).get("role").asText()).isEqualTo("USER");
        assertThat(messages.get(1).get("role").asText()).isEqualTo("ASSISTANT");
    }

    @Test
    @DisplayName("a budget question reports the configured limit, not an invented one")
    void answersBudgetQuestion() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", foodCategoryId,
                                "amount", "3000000.00",
                                "periodType", "MONTHLY",
                                "periodStart", LocalDate.now().withDayOfMonth(1).toString()))))
                .andExpect(status().isCreated());

        JsonNode response = ask("Am I over budget?");

        assertThat(response.get("intent").asText()).isEqualTo("BUDGET_STATUS");
        assertThat(response.get("answer").asText()).contains("3.000.000");
    }

    @Test
    @DisplayName("another user cannot read the conversation transcript")
    void hidesTranscriptsFromOtherUsers() throws Exception {
        long conversationId = ask("This month, where did I spend the most?").get("conversationId").asLong();

        String stamp = UUID.randomUUID().toString().substring(0, 8);
        MvcResult other = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "other." + stamp + "@example.com",
                                "username", "other_" + stamp,
                                "password", "Password1"))))
                .andExpect(status().isCreated())
                .andReturn();
        String otherToken = json(other).get("accessToken").asText();

        mockMvc.perform(get("/api/ai/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an empty question is rejected with 400")
    void rejectsEmptyQuestion() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the assistant never leaks another account's figures")
    void answersAreScopedToTheCaller() throws Exception {
        JsonNode mine = ask("This month, where did I spend the most?");
        String myAnswer = mine.get("answer").asText();

        // The bootstrap admin has no transactions, so its answer must be a refusal.
        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", "admin@finai.local",
                                "password", "Admin#12345"))))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(adminLogin.getResponse().getStatus())
                .withFailMessage("admin login body was: %s", adminLogin.getResponse().getContentAsString())
                .isEqualTo(200);
        String adminToken = json(adminLogin).get("accessToken").asText();

        MvcResult adminAnswer = mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "This month, where did I spend the most?"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminJson = json(adminAnswer);
        assertThat(adminJson.get("grounded").asBoolean()).isFalse();
        assertThat(adminJson.get("answer").asText())
                .doesNotContain("4.800.000")
                .isNotEqualTo(myAnswer);
    }
}
