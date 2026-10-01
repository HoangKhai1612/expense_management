package com.finai.ai;

/** The question shapes the analyst knows how to answer from verified data. */
public enum AiIntent {
    /** "Where am I spending the most?" */
    SPENDING_ANALYSIS,
    /** "Am I over budget this month?" */
    BUDGET_STATUS,
    /** "How does this month compare with last month?" */
    PERIOD_COMPARISON,
    /** "Which category is trending up?" */
    TREND,
    /** "What is my income situation?" */
    INCOME_ANALYSIS,
    /** "How am I doing overall?" */
    OVERVIEW,
    /** Nothing recognised: answer with capabilities and the data that does exist. */
    UNRECOGNISED
}
