package com.finai.ai;

import com.finai.ai.AiDataSnapshotService.Snapshot;
import com.finai.config.BudgetAlertProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The built-in analyst.
 *
 * Every sentence it can emit is assembled from {@link AiDataSnapshotService.Snapshot}
 * values, and each numeric claim is simultaneously emitted into a `facts` list that
 * is persisted with the message. That gives the audit trail a checkable link
 * between what the user was told and what the database actually held.
 *
 * The answer is always split into FACT (verified) and SUGGESTION (judgement), so a
 * recommendation can never be mistaken for a measurement.
 */
class AiAnalyst {

    private final BudgetAlertProperties alertProperties;

    AiAnalyst(BudgetAlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    record Answer(String content, List<String> facts, boolean grounded) {
    }

    Answer answer(String question, AiIntent intent, AiDataSnapshotService.Snapshot snapshot, boolean vietnamese) {
        if (!snapshot.hasAnyData()) {
            return noData(vietnamese);
        }
        return switch (intent) {
            case OVERVIEW -> overview(snapshot, vietnamese);
            case SPENDING_ANALYSIS -> spending(snapshot, vietnamese);
            case BUDGET_STATUS -> budget(snapshot, vietnamese);
            case PERIOD_COMPARISON -> comparison(snapshot, vietnamese);
            case TREND -> trend(snapshot, vietnamese);
            case INCOME_ANALYSIS -> income(snapshot, vietnamese);
            case UNRECOGNISED -> unrecognised(snapshot, vietnamese);
        };
    }

    // ---------------------------------------------------------------- overview

    private Answer overview(Snapshot snapshot, boolean vi) {
        List<String> facts = new ArrayList<>();
        List<String> lines = new ArrayList<>();

        facts.add(fact("period", snapshot.currentMonth().toString()));
        facts.add(fact("income", snapshot.income()));
        facts.add(fact("expense", snapshot.expense()));
        facts.add(fact("balance", snapshot.balance()));
        facts.add(fact("transactionCount", snapshot.transactionCount()));

        lines.add(vi
                ? "Dưới đây là số liệu thực tế của bạn trong tháng " + snapshot.currentMonth() + ":"
                : "Here is your actual data for " + snapshot.currentMonth() + ":");
        lines.add(money(snapshot.income(), vi) + " " + (vi ? "thu nhập" : "income"));
        lines.add(money(snapshot.expense(), vi) + " " + (vi ? "chi tiêu" : "expenses"));
        lines.add(money(snapshot.balance(), vi) + " " + (vi ? "số dư" : "balance"));
        lines.add(vi
                ? "Hệ thống đang lưu " + snapshot.transactionCount() + " giao dịch cho tài khoản này."
                : "The system holds " + snapshot.transactionCount() + " transactions for this account.");

        List<String> suggestions = new ArrayList<>();
        if (snapshot.balance().signum() < 0) {
            suggestions.add(vi
                    ? "Bạn đang chi nhiều hơn thu nhập. Cân nhắc giảm hai danh mục chi lớn nhất trước."
                    : "You are spending more than you earn. Consider trimming the two largest "
                            + "expense categories first.");
        }
        suggestions.add(vi
                ? "Đây là số liệu thực tế từ dữ liệu bạn đã nhập, không phải ước tính."
                : "These are actual figures taken from the data you entered, not estimates.");

        return new Answer(render(lines, suggestions), facts, true);
    }

    // ------------------------------------------------------- spending analysis

    private Answer spending(Snapshot snapshot, boolean vi) {
        if (snapshot.topSpending().isEmpty()) {
            return noSpendingInPeriod(snapshot, vi);
        }
        AiDataSnapshotService.CategoryTotal top = snapshot.topSpending().get(0);
        BigDecimal share = share(top.amount(), snapshot.expense());

        List<String> facts = new ArrayList<>(
                List.of(
                        fact("period", snapshot.currentMonth().toString()),
                        fact("topCategory", top.name()),
                        fact("topCategoryAmount", top.amount()),
                        fact("topCategoryTransactions", top.count()),
                        fact("totalExpense", snapshot.expense()),
                        fact("topCategorySharePercentage", share)));

        List<String> lines = new ArrayList<>();
        lines.add(vi
                ? "Bạn chi nhiều nhất vào \"" + top.name() + "\" trong tháng " + snapshot.currentMonth() + "."
                : "You spend the most on \"" + top.name() + "\" in " + snapshot.currentMonth() + ".");
        lines.add(vi
                ? "Tổng chi tiêu tháng đó là " + money(snapshot.expense(), vi) + ", trong đó "
                + money(top.amount(), vi) + " (" + plain(share) + "%) thuộc danh mục này, "
                + "từ " + top.count() + " giao dịch."
                : "Total expenses that month were " + money(snapshot.expense(), vi) + ", of which "
                + money(top.amount(), vi) + " (" + plain(share) + "%) were in this category, "
                + "across " + top.count() + " transactions.");

        if (snapshot.topSpending().size() > 1) {
            StringBuilder rest = new StringBuilder(vi ? "Các danh mục còn lại: " : "Other categories: ");
            for (int i = 1; i < snapshot.topSpending().size(); i++) {
                AiDataSnapshotService.CategoryTotal item = snapshot.topSpending().get(i);
                if (i > 1) {
                    rest.append(", ");
                }
                rest.append(item.name()).append(" ").append(money(item.amount(), vi));
            }
            rest.append(".");
            lines.add(rest.toString());
        }

        BigDecimal dailyAverage = snapshot.expense()
                .divide(BigDecimal.valueOf(snapshot.currentMonth().lengthOfMonth()), 2, RoundingMode.HALF_UP);
        facts.add(fact("averageDailyExpense", dailyAverage));
        lines.add(vi
                ? "Trung bình mỗi ngày trong tháng bạn chi khoảng " + money(dailyAverage, vi) + "."
                : "On average you spent about " + money(dailyAverage, vi) + " per day that month.");

        List<String> suggestions = new ArrayList<>();
        if (share.compareTo(new BigDecimal("40")) > 0) {
            suggestions.add(vi
                    ? "\"" + top.name() + "\" chiếm hơn 40% chi tiêu của bạn; đây là nơi điều chỉnh sẽ hiệu quả nhất."
                    : "\"" + top.name() + "\" is over 40% of your spending, so it is where a change would have the most effect.");
        }
        suggestions.add(vi
                ? "Đây là số liệu thực tế từ dữ liệu bạn đã nhập, không phải ước tính."
                : "These are actual figures taken from the data you entered, not estimates.");
        if (vi) {
            suggestions.add("Đây là gợi ý, không phải sự thật đã được kiểm chứng.");
        } else {
            suggestions.add("The above is a suggestion, not a verified fact.");
        }

        return new Answer(render(lines, suggestions), facts, true);
    }

    // ------------------------------------------------------------ budget status

    private Answer budget(Snapshot snapshot, boolean vi) {
        if (snapshot.budgets().isEmpty()) {
            return noBudgets(snapshot, vi);
        }

        List<String> facts = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        facts.add(fact("period", snapshot.currentMonth().toString()));
        facts.add(fact("budgetCount", snapshot.budgets().size()));

        lines.add(vi
                ? "Trong tháng " + snapshot.currentMonth() + " bạn đang theo dõi "
                + snapshot.budgets().size() + " ngân sách:"
                : "In " + snapshot.currentMonth() + " you are tracking "
                + snapshot.budgets().size() + " budgets:");

        int exceeded = 0;
        int warning = 0;
        BigDecimal overspend = BigDecimal.ZERO;

        for (AiDataSnapshotService.BudgetFact budget : snapshot.budgets()) {
            boolean isExceeded = budget.usagePercentage()
                    .compareTo(alertProperties.getExceededThreshold()) >= 0;
            boolean isWarning = !isExceeded && budget.usagePercentage()
                    .compareTo(alertProperties.getWarningThreshold()) >= 0;

            facts.add(fact("budget:" + budget.id(), budget.limit()));
            facts.add(fact("budgetUsed:" + budget.id(), budget.used()));
            facts.add(fact("budgetUsagePercentage:" + budget.id(), budget.usagePercentage()));

            String status = isExceeded
                    ? (vi ? "VƯỢT" : "EXCEEDED")
                    : isWarning ? (vi ? "CẢNH BÁO" : "WARNING") : (vi ? "trong hạn mức" : "within limit");
            lines.add("- " + budget.categoryName() + ": " + money(budget.used(), vi) + " / "
                    + money(budget.limit(), vi) + " (" + plain(budget.usagePercentage()) + "%) - " + status);

            if (isExceeded) {
                exceeded++;
                overspend = overspend.add(budget.used().subtract(budget.limit()));
            } else if (isWarning) {
                warning++;
            }
        }

        facts.add(fact("exceededBudgetCount", exceeded));
        facts.add(fact("warningBudgetCount", warning));

        if (exceeded > 0) {
            lines.add(vi
                    ? "Kết luận: bạn ĐÃ vượt " + exceeded + " ngân sách, tổng phần vượt là "
                    + money(overspend, vi) + "."
                    : "Conclusion: you HAVE exceeded " + exceeded + " budget(s) by a combined "
                    + money(overspend, vi) + ".");
            suggestions.add(vi
                    ? "Giảm chi ở danh mục vượt nhiều nhất sẽ đem lại hiệu quả nhanh nhất."
                    : "Reducing the category you overshot the most will have the fastest effect.");
        } else if (warning > 0) {
            lines.add(vi
                    ? "Kết luận: bạn CHƯA vượt ngân sách, nhưng có " + warning
                    + " ngân sách đã vượt ngưỡng cảnh báo " + plain(alertProperties.getWarningThreshold()) + "%."
                    : "Conclusion: you have NOT exceeded any budget, but " + warning + " of them are past the "
                    + plain(alertProperties.getWarningThreshold()) + "% warning threshold.");
            suggestions.add(vi
                    ? "Ngưỡng cảnh báo được cấu hình ở " + plain(alertProperties.getWarningThreshold())
                    + "%, ngưỡng vượt ở " + plain(alertProperties.getExceededThreshold()) + "%."
                    : "The warning threshold is configured at " + plain(alertProperties.getWarningThreshold())
                    + "% and the exceeded threshold at " + plain(alertProperties.getExceededThreshold()) + "%.");
        } else {
            lines.add(vi
                    ? "Kết luận: mọi ngân sách đều đang trong hạn mức."
                    : "Conclusion: every budget is still within its limit.");
        }

        return new Answer(render(lines, suggestions), facts, true);
    }

    // -------------------------------------------------------------- comparison

    private Answer comparison(Snapshot snapshot, boolean vi) {
        if (!hasAnyPreviousActivity(snapshot)) {
            return noPreviousPeriod(vi);
        }

        List<String> facts = new ArrayList<>();
        facts.add(fact("currentPeriod", snapshot.currentMonth().toString()));
        facts.add(fact("previousPeriod", snapshot.previousMonth().toString()));
        facts.add(fact("currentExpense", snapshot.expense()));
        facts.add(fact("previousExpense", snapshot.previousExpense()));
        facts.add(fact("currentIncome", snapshot.income()));
        facts.add(fact("previousIncome", snapshot.previousIncome()));

        List<String> lines = new ArrayList<>();
        lines.add(vi
                ? "So sánh chi tiêu tháng " + snapshot.currentMonth() + " với tháng " + snapshot.previousMonth() + ":"
                : "Spending in " + snapshot.currentMonth() + " compared with " + snapshot.previousMonth() + ":");

        lines.add(vi
                ? "- Chi tiêu: " + money(snapshot.expense(), vi) + " so với " + money(snapshot.previousExpense(), vi)
                + " (" + describeChange(snapshot.expense(), snapshot.previousExpense(), vi) + ")"
                : "- Expenses: " + money(snapshot.expense(), vi) + " vs " + money(snapshot.previousExpense(), vi)
                + " (" + describeChange(snapshot.expense(), snapshot.previousExpense(), vi) + ")");
        lines.add(vi
                ? "- Thu nhập: " + money(snapshot.income(), vi) + " so với " + money(snapshot.previousIncome(), vi)
                + " (" + describeChange(snapshot.income(), snapshot.previousIncome(), vi) + ")"
                : "- Income: " + money(snapshot.income(), vi) + " vs " + money(snapshot.previousIncome(), vi)
                + " (" + describeChange(snapshot.income(), snapshot.previousIncome(), vi) + ")");

        int direction = snapshot.expense().compareTo(snapshot.previousExpense());
        List<String> suggestions = new ArrayList<>();
        if (direction > 0) {
            suggestions.add(vi
                    ? "Chi tiêu đang tăng. Danh mục tăng nhiều nhất so với tháng trước là nơi nên xem lại trước tiên."
                    : "Spending is rising. Look first at whichever category grew the most versus last month.");
        } else if (direction < 0) {
            suggestions.add(vi
                    ? "Chi tiêu đang giảm so với tháng trước."
                    : "Spending has fallen compared with the previous month.");
        }

        return new Answer(render(lines, suggestions), facts, true);
    }

    // -------------------------------------------------------------------- trend

    private Answer trend(Snapshot snapshot, boolean vi) {
        if (snapshot.spendingTrend().isEmpty()) {
            return noSpendingInPeriod(snapshot, vi);
        }

        List<String> facts = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        facts.add(fact("currentPeriod", snapshot.currentMonth().toString()));
        facts.add(fact("previousPeriod", snapshot.previousMonth().toString()));

        lines.add(vi
                ? "So với tháng " + snapshot.previousMonth() + ", mức tăng/giảm theo danh mục trong tháng "
                + snapshot.currentMonth() + " là:"
                : "Compared with " + snapshot.previousMonth() + ", the change per category in "
                + snapshot.currentMonth() + " is:");

        String biggestIncrease = null;
        BigDecimal biggestIncreaseValue = BigDecimal.ZERO;
        int rising = 0;

        for (Map.Entry<String, BigDecimal> entry : snapshot.spendingTrend().entrySet()) {
            BigDecimal delta = entry.getValue();
            facts.add(fact("trend:" + entry.getKey(), delta));
            lines.add("- " + entry.getKey() + ": " + signedMoney(delta, vi));
            if (delta.signum() > 0) {
                rising++;
                if (delta.compareTo(biggestIncreaseValue) > 0) {
                    biggestIncreaseValue = delta;
                    biggestIncrease = entry.getKey();
                }
            }
        }

        if (rising == 0) {
            lines.add(vi
                    ? "Không có danh mục nào tăng chi tiêu so với tháng trước."
                    : "No category increased its spending compared with the previous month.");
        } else {
            lines.add(vi
                    ? "Có " + rising + " danh mục tăng; tăng nhiều nhất là \"" + biggestIncrease
                    + "\" với " + signedMoney(biggestIncreaseValue, vi) + "."
                    : rising + " category/categories increased; the largest rise is \"" + biggestIncrease
                    + "\" at " + signedMoney(biggestIncreaseValue, vi) + ".");
            suggestions.add(vi
                    ? "Xu hướng tăng ở \"" + biggestIncrease + "\" là tín hiệu sớm, xử lý trước khi nó thành thói quen."
                    : "The rise in \"" + biggestIncrease + "\" is an early signal worth acting on before it becomes a habit.");
        }

        return new Answer(render(lines, suggestions), facts, true);
    }

    // ------------------------------------------------------------ income analysis

    private Answer income(Snapshot snapshot, boolean vi) {
        if (snapshot.income().signum() == 0 && snapshot.topIncome().isEmpty()) {
            List<String> lines = List.of(vi
                    ? "Trong tháng " + snapshot.currentMonth() + " hệ thống không ghi nhận khoản thu nhập nào cho tài khoản của bạn."
                    : "No income was recorded for your account in " + snapshot.currentMonth() + ".");
            return new Answer(render(lines, List.of(vi
                    ? "Hãy thêm các khoản thu để phân tích thu nhập chính xác hơn."
                    : "Add your income entries for a more accurate income analysis.")),
                    List.of(fact("income", snapshot.income())), true);
        }

        List<String> facts = new ArrayList<>();
        facts.add(fact("period", snapshot.currentMonth().toString()));
        facts.add(fact("income", snapshot.income()));
        facts.add(fact("expense", snapshot.expense()));
        facts.add(fact("balance", snapshot.balance()));

        List<String> lines = new ArrayList<>();
        lines.add(vi
                ? "Thu nhập tháng " + snapshot.currentMonth() + " của bạn là " + money(snapshot.income(), vi) + "."
                : "Your income in " + snapshot.currentMonth() + " was " + money(snapshot.income(), vi) + ".");
        if (!snapshot.topIncome().isEmpty()) {
            AiDataSnapshotService.CategoryTotal top = snapshot.topIncome().get(0);
            facts.add(fact("topIncomeCategory", top.name()));
            facts.add(fact("topIncomeAmount", top.amount()));
            lines.add(vi
                    ? "Nguồn thu lớn nhất là \"" + top.name() + "\" với " + money(top.amount(), vi) + "."
                    : "The largest source is \"" + top.name() + "\" at " + money(top.amount(), vi) + ".");
        }
        lines.add(vi
                ? "Sau khi trừ " + money(snapshot.expense(), vi) + " chi tiêu, phần còn lại là "
                + money(snapshot.balance(), vi) + "."
                : "After " + money(snapshot.expense(), vi) + " of expenses, " + money(snapshot.balance(), vi)
                + " remains.");

        BigDecimal savingsRate = snapshot.income().signum() == 0
                ? BigDecimal.ZERO
                : snapshot.balance().multiply(new BigDecimal("100"))
                .divide(snapshot.income(), 2, RoundingMode.HALF_UP);
        facts.add(fact("savingsRatePercentage", savingsRate));
        lines.add(vi
                ? "Tỉ lệ tiết kiệm so với thu nhập là " + plain(savingsRate) + "%."
                : "Your savings rate against income is " + plain(savingsRate) + "%.");

        return new Answer(render(lines, List.of(vi
                ? "Tỉ lệ tiết kiệm là chỉ số tham khảo, không phải mục tiêu tài chính chuẩn."
                : "The savings rate is an indicator, not a financial target.")),
                facts, true);
    }

    // ------------------------------------------------------------- unrecognised

    private Answer unrecognised(Snapshot snapshot, boolean vi) {
        List<String> facts = new ArrayList<>();
        List<String> lines = new ArrayList<>();

        facts.add(fact("period", snapshot.currentMonth().toString()));
        facts.add(fact("transactionCount", snapshot.transactionCount()));
        facts.add(fact("income", snapshot.income()));
        facts.add(fact("expense", snapshot.expense()));

        lines.add(vi
                ? "Tôi chưa hiểu câu hỏi này một cách chắc chắn, nên tôi sẽ không đoán. Đây là những gì dữ liệu của bạn cho tôi biết trong tháng "
                + snapshot.currentMonth() + ":"
                : "I could not classify that question confidently, so I will not guess. Here is what your data shows for "
                + snapshot.currentMonth() + ":");
        lines.add(money(snapshot.income(), vi) + " " + (vi ? "thu nhập" : "income"));
        lines.add(money(snapshot.expense(), vi) + " " + (vi ? "chi tiêu" : "expenses"));
        lines.add(money(snapshot.balance(), vi) + " " + (vi ? "số dư" : "balance"));

        lines.add(vi
                ? "Tôi có thể trả lời chắc chắn các câu hỏi như: tôi tiêu nhiều nhất vào đâu; tháng này tôi có vượt ngân sách không; "
                + "chi tiêu so với tháng trước thế nào; chi tiêu đang tăng ở nhóm nào; thu nhập của tôi ra sao."
                : "I can answer these reliably: where I spend the most; am I over budget this month; "
                + "how does this month compare with the last; which category is trending up; what is my income.");

        return new Answer(render(lines, List.of()), facts, true);
    }

    // ---------------------------------------------------------------- refusals

    private Answer noData(boolean vi) {
        String content = render(
                List.of(vi
                        ? "Tôi chưa có đủ dữ liệu để trả lời. Tài khoản của bạn chưa có giao dịch nào được ghi nhận, "
                        + "nên tôi không thể đưa ra bất kỳ con số nào một cách trung thực."
                        : "I do not have enough data to answer. No transactions have been recorded for your account, "
                        + "so I cannot honestly give you any figure."),
                List.of(vi
                        ? "Hãy thêm ít nhất một khoản thu và một khoản chi, rồi hỏi lại tôi."
                        : "Add at least one income and one expense entry, then ask me again."));
        // grounded = false: no database fact was used, so no number may be claimed.
        return new Answer(content, List.of(fact("dataAvailable", false)), false);
    }

    private Answer noSpendingInPeriod(Snapshot snapshot, boolean vi) {
        String content = render(
                List.of(vi
                        ? "Trong tháng " + snapshot.currentMonth() + " hệ thống không ghi nhận khoản chi nào, "
                        + "nên tôi không thể chỉ ra danh mục bạn chi nhiều nhất."
                        : "No expense was recorded in " + snapshot.currentMonth()
                        + ", so I cannot tell you which category you spent the most on."),
                List.of(vi
                        ? "Tôi sẽ không ước đoán số liệu chưa có trong hệ thống."
                        : "I will not estimate a figure that is not in the system."));
        return new Answer(content, List.of(fact("expenseThisPeriod", snapshot.expense())), true);
    }

    private Answer noBudgets(Snapshot snapshot, boolean vi) {
        String content = render(
                List.of(vi
                        ? "Bạn chưa tạo ngân sách nào cho tháng " + snapshot.currentMonth()
                        + ", nên tôi không thể trả lời câu hỏi về việc vượt ngân sách."
                        : "You have not created any budget for " + snapshot.currentMonth()
                        + ", so I cannot answer whether you are over budget."),
                List.of(vi
                        ? "Hãy tạo ngân sách theo danh mục, rồi hỏi lại tôi."
                        : "Create a per-category budget and ask me again."));
        return new Answer(content, List.of(fact("budgetCount", 0)), true);
    }

    private Answer noPreviousPeriod(boolean vi) {
        String content = render(
                List.of(vi
                        ? "Tôi không có dữ liệu tháng trước để so sánh, nên tôi sẽ không bịa ra mức tăng hay giảm."
                        : "I have no data for the previous month to compare against, so I will not invent a change."),
                List.of(vi
                        ? "Hãy nhập giao dịch cho ít nhất một tháng trước rồi hỏi lại."
                        : "Record transactions for at least one earlier month, then ask again."));
        return new Answer(content, List.of(), false);
    }

    private boolean hasAnyPreviousActivity(Snapshot snapshot) {
        return snapshot.previousIncome().signum() > 0 || snapshot.previousExpense().signum() > 0;
    }

    // ------------------------------------------------------------- formatting

    private String render(List<String> factLines, List<String> suggestionLines) {
        StringBuilder builder = new StringBuilder();
        builder.append("FACT\n");
        if (factLines.isEmpty()) {
            builder.append("- (no verified facts available)\n");
        } else {
            factLines.forEach(line -> builder.append("- ").append(line).append('\n'));
        }
        builder.append("\nSUGGESTION\n");
        if (suggestionLines.isEmpty()) {
            builder.append("- (none)\n");
        } else {
            suggestionLines.forEach(line -> builder.append("- ").append(line).append('\n'));
        }
        return builder.toString().trim();
    }

    private List<String> linesOfSuggestions(boolean vi, String... pairs) {
        List<String> result = new ArrayList<>();
        result.add(vi ? pairs[0] : pairs[1]);
        return result;
    }

    private String describeChange(BigDecimal current, BigDecimal previous, boolean vi) {
        if (previous.signum() == 0) {
            return vi ? "không có dữ liệu tháng trước để so sánh" : "no prior figure to compare against";
        }
        BigDecimal change = current.subtract(previous);
        BigDecimal percentage = change.multiply(new BigDecimal("100"))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
        String sign = change.signum() > 0 ? "+" : "";
        return sign + plain(percentage) + "%";
    }

    private BigDecimal share(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(new BigDecimal("100")).divide(whole, 2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value, boolean vi) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        // Handle the sign separately: grouping a leading '-' into the digits would
        // otherwise produce "-.1.234".
        String sign = safe.signum() < 0 ? "-" : "";
        BigDecimal absolute = safe.abs().stripTrailingZeros();

        String digits;
        if (absolute.scale() <= 0) {
            digits = absolute.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        } else {
            digits = absolute.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }

        int dot = digits.indexOf('.');
        String integerPart = dot < 0 ? digits : digits.substring(0, dot);
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < integerPart.length(); i++) {
            if (i > 0 && (integerPart.length() - i) % 3 == 0) {
                grouped.append('.');
            }
            grouped.append(integerPart.charAt(i));
        }
        String body = dot < 0 ? grouped.toString() : grouped + digits.substring(dot);
        return sign + body + (vi ? "đ" : " VND");
    }

    private String signedMoney(BigDecimal value, boolean vi) {
        String prefix = value.signum() > 0 ? "+" : "";
        return prefix + money(value, vi);
    }

    private String plain(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String fact(String key, Object value) {
        return key + "=" + value;
    }
}
