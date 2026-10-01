package com.finai.ai;

import java.util.List;
import java.util.Locale;

/**
 * Minimal, deterministic language and intent detection.
 *
 * There is no machine-learned classifier here on purpose: the mapping from a
 * question to an intent is part of the product contract, it must be testable, and
 * a misclassification must degrade to UNRECOGNISED rather than to a wrong number.
 */
final class AiText {

    private static final String VIETNAMESE_DIACRITICS =
            "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩị"
            + "òóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
            + "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊ"
            + "ÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ";

    private AiText() {
    }

    static boolean isVietnamese(String question) {
        for (int i = 0; i < question.length(); i++) {
            if (VIETNAMESE_DIACRITICS.indexOf(question.charAt(i)) >= 0) {
                return true;
            }
        }
        String normalised = question.toLowerCase(Locale.ROOT);
        for (String keyword : List.of("tôi", "toi", "thang", "tháng", "chi tieu", "chi tiêu",
                "ngân sách", "ngan sach", "thu nhập", "thu nhap", "bao nhiêu", "hieu")) {
            if (normalised.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Classifies the question. Order matters: the more specific shapes are tested
     * before the general overview, and anything unrecognised falls through to
     * UNRECOGNISED rather than guessing.
     */
    static AiIntent detectIntent(String question) {
        String text = question.toLowerCase(Locale.ROOT);

        boolean mentionsComparison = containsAny(text,
                "so với", "so voi", "so sanh", "so sánh", "compare", "vs", "khác", "khac",
                "tháng trước", "thang truoc", "last month", "previous month", "trước", "truoc");
        boolean mentionsBudget = containsAny(text,
                "ngân sách", "ngan sach", "budget", "vượt", "vuot", "exceed", "over budget", "hạn mức", "han muc");
        boolean mentionsIncome = containsAny(text,
                "thu nhập", "thu nhap", "income", "salary", "lương", "luong", "earn", "kiếm", "kiem");
        boolean mentionsTrend = containsAny(text,
                "xu hướng", "xu huong", "trend", "tăng", "tang", "đang tăng", "dang tang",
                "gần đây", "gan day", "recently", "lâu dần");
        // "most" / "where" / "spend" are checked before the generic period words, so a
        // question like "this month, where did I spend the most?" is answered as a
        // spending question rather than being swallowed by the overview branch.
        boolean mentionsMost = containsAny(text,
                "nhiều nhất", "nhieu nhat", "most", "largest", "biggest", "top", "where",
                "đâu", " dau", "chi vào");
        boolean mentionsSpending = containsAny(text,
                "spend", "spending", "expense", "expenses", "chi tiêu", "chi tieu", "chi vào", "mua");

        if (mentionsBudget) {
            return AiIntent.BUDGET_STATUS;
        }
        if (mentionsComparison) {
            return AiIntent.PERIOD_COMPARISON;
        }
        if (mentionsTrend) {
            return AiIntent.TREND;
        }
        if (mentionsIncome) {
            return AiIntent.INCOME_ANALYSIS;
        }
        if (mentionsMost || mentionsSpending) {
            return AiIntent.SPENDING_ANALYSIS;
        }
        if (containsAny(text, "tổng quan", "tong quan", "overview", "summary", "tóm tắt", "tom tat",
                "tôi thu chi", "toi thu chi", "tháng này", "thang nay", "this month",
                "how am i", "bao nhiêu", "balance", "số dư", "so du")) {
            return AiIntent.OVERVIEW;
        }
        return AiIntent.UNRECOGNISED;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
