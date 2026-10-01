package com.finai.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Intent classification is part of the product contract, so it is pinned here
 * rather than left to an end-to-end test that would only report a symptom.
 */
class AiTextTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Tháng này tôi chi tiêu bao nhiêu?|SPENDING_ANALYSIS",
            "Tổng quan tháng này của tôi?|OVERVIEW",
            "How much is my balance this month?|OVERVIEW",
            "This month, where did I spend the most?|SPENDING_ANALYSIS",
            "Where did my money go this month?|SPENDING_ANALYSIS",
            "Tôi chi nhiều nhất vào mục nào?|SPENDING_ANALYSIS",
            "Phân tích chi tiêu của tôi tháng này|SPENDING_ANALYSIS",
            "Mình có vượt ngân sách không?|BUDGET_STATUS",
            "Am I over budget?|BUDGET_STATUS",
            "So sánh với tháng trước|PERIOD_COMPARISON",
            "How does this month compare to last month?|PERIOD_COMPARISON",
            "Thu nhập của tôi tháng này thế nào?|INCOME_ANALYSIS",
            "What is my income?|INCOME_ANALYSIS",
            "Chi tiêu của tôi có xu hướng tăng không?|TREND",
            "Is my spending trending up recently?|TREND"
    })
    @DisplayName("classifies the documented question shapes")
    void classifiesKnownQuestions(String question, AiIntent expected) {
        assertThat(AiText.detectIntent(question.trim())).isEqualTo(expected);
    }

    /**
     * Regression: the general overview branch used to swallow this question, so
     * "where did I spend the most" was answered with a flat month total instead of
     * the top category. The "most"/"where" checks must stay ahead of the period words.
     */
    @Test
    @DisplayName("a spending question carrying a month word is not downgraded to overview")
    void monthWordDoesNotMaskTopCategoryQuestion() {
        assertThat(AiText.detectIntent("This month, where did I spend the most?"))
                .isEqualTo(AiIntent.SPENDING_ANALYSIS);
    }

    @Test
    @DisplayName("budget beats comparison when a question mentions both")
    void budgetWinsOverComparison() {
        assertThat(AiText.detectIntent("Did I go over budget compared to last month?"))
                .isEqualTo(AiIntent.BUDGET_STATUS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "What is the capital of France?",
            "hello there",
            "12345"
    })
    @DisplayName("an unrecognised question degrades to UNRECOGNISED rather than a guess")
    void unrecognisedQuestionsDegradeSafely(String question) {
        assertThat(AiText.detectIntent(question)).isEqualTo(AiIntent.UNRECOGNISED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Tôi chi tiêu bao nhiêu?",
            "thang nay",
            "Mình có ngân sách không"
    })
    @DisplayName("recognises Vietnamese with or without diacritics")
    void detectsVietnamese(String question) {
        assertThat(AiText.isVietnamese(question)).isTrue();
    }

    @Test
    @DisplayName("an English-only question is not treated as Vietnamese")
    void englishIsNotVietnamese() {
        assertThat(AiText.isVietnamese("How much did I spend this month?")).isFalse();
    }
}
