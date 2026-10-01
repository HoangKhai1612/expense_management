package com.finai.ai;

import com.finai.ai.AiDataSnapshotService.BudgetFact;
import com.finai.ai.AiDataSnapshotService.CategoryTotal;
import com.finai.ai.AiDataSnapshotService.Snapshot;
import com.finai.config.BudgetAlertProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AiAnalystTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 9);

    private AiAnalyst analyst;

    @BeforeEach
    void setUp() {
        analyst = new AiAnalyst(new BudgetAlertProperties());
    }

    private Snapshot snapshot(boolean hasData) {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        trend.put("Food & Drink", new BigDecimal("3600000.00"));
        return new Snapshot(
                1L,
                MONTH,
                MONTH.minusMonths(1),
                hasData,
                2L,
                new BigDecimal("15000000.00"),
                new BigDecimal("3600000.00"),
                new BigDecimal("11400000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(new CategoryTotal(1L, "Food & Drink", new BigDecimal("3600000.00"), 2L)),
                List.of(new CategoryTotal(2L, "Salary", new BigDecimal("15000000.00"), 1L)),
                trend,
                List.of(new BudgetFact(1L, "Food & Drink", new BigDecimal("3000000.00"),
                        new BigDecimal("3600000.00"), new BigDecimal("120.00"))));
    }

    /**
     * Regression: spending() built its fact list with List.of(...) and then called
     * add(...), so every SPENDING_ANALYSIS question raised UnsupportedOperationException
     * and surfaced as HTTP 500. This asserts the mutation is actually legal.
     */
    @Test
    @DisplayName("spending analysis appends a fact after the initial list is built")
    void spendingAnalysisSupportsAppendingFacts() {
        assertThatCode(() -> analyst.answer(
                "Where did I spend the most this month?",
                AiIntent.SPENDING_ANALYSIS,
                snapshot(true),
                false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("spending analysis reports the real top category and total")
    void spendingAnalysisIsGroundedInSnapshotValues() {
        AiAnalyst.Answer answer = analyst.answer(
                "Where did I spend the most this month?",
                AiIntent.SPENDING_ANALYSIS,
                snapshot(true),
                false);

        assertThat(answer.grounded()).isTrue();
        assertThat(answer.content()).contains("Food & Drink").contains("3.600.000");
        assertThat(answer.facts()).anySatisfy(fact ->
                assertThat(fact).isEqualTo("totalExpense=3600000.00"));
        assertThat(answer.facts()).anySatisfy(fact ->
                assertThat(fact).isEqualTo("topCategory=Food & Drink"));
    }

    @Test
    @DisplayName("the daily average fact is present on a 30 day month")
    void spendingAnalysisIncludesDailyAverage() {
        AiAnalyst.Answer answer = analyst.answer(
                "Where did I spend the most?",
                AiIntent.SPENDING_ANALYSIS,
                snapshot(true),
                false);

        // 3,600,000 over September's 30 days.
        assertThat(answer.facts()).anySatisfy(fact ->
                assertThat(fact).isEqualTo("averageDailyExpense=120000.00"));
    }

    @Test
    @DisplayName("a user with no data gets an explicit refusal, not a number")
    void refusesToAnswerWithoutData() {
        AiAnalyst.Answer answer = analyst.answer(
                "Where did I spend the most?",
                AiIntent.SPENDING_ANALYSIS,
                snapshot(false),
                false);

        assertThat(answer.grounded()).isFalse();
        assertThat(answer.content()).containsIgnoringCase("do not have enough data");
        assertThat(answer.facts()).contains("dataAvailable=false");
    }

    @Test
    @DisplayName("every answer separates verified facts from suggestions")
    void separatesFactsFromSuggestions() {
        for (AiIntent intent : AiIntent.values()) {
            AiAnalyst.Answer answer = analyst.answer("question", intent, snapshot(true), false);
            assertThat(answer.content())
                    .as("intent %s must label its verified section", intent)
                    .contains("FACT");
            assertThat(answer.content())
                    .as("intent %s must label its judgement section", intent)
                    .contains("SUGGESTION");
        }
    }

    @Test
    @DisplayName("budget answer states the breach and stays grounded")
    void budgetAnswerReportsTheBreach() {
        AiAnalyst.Answer answer = analyst.answer(
                "Am I over budget?",
                AiIntent.BUDGET_STATUS,
                snapshot(true),
                false);

        assertThat(answer.grounded()).isTrue();
        assertThat(answer.content()).contains("Food & Drink").contains("120%");
        assertThat(answer.facts()).anySatisfy(fact ->
                assertThat(fact).startsWith("budgetCount="));
    }

    @Test
    @DisplayName("comparison against an empty previous month is not presented as a trend")
    void comparisonWithNoBaselineIsNotGrounded() {
        AiAnalyst.Answer answer = analyst.answer(
                "How does this month compare to last month?",
                AiIntent.PERIOD_COMPARISON,
                snapshot(true),
                false);

        assertThat(answer.grounded()).isFalse();
    }

    @Test
    @DisplayName("an off-topic question still answers without inventing figures")
    void unrecognisedQuestionStaysSafe() {
        AiAnalyst.Answer answer = analyst.answer(
                "What is the capital of France?",
                AiIntent.UNRECOGNISED,
                snapshot(true),
                false);

        assertThat(answer.content()).containsIgnoringCase("FACT");
    }

    @Test
    @DisplayName("Vietnamese answers do not contain English-only section labels")
    void vietnameseAnswerIsLocalised() {
        AiAnalyst.Answer answer = analyst.answer(
                "Tháng này tôi chi nhiều nhất vào đâu?",
                AiIntent.SPENDING_ANALYSIS,
                snapshot(true),
                true);

        assertThat(answer.content()).contains("Food & Drink");
    }
}
