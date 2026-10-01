package com.finai.statistics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "Aggregations computed from the caller's own transactions")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Income, expense and balance for a window",
            description = "Defaults to the current calendar month. Returns zeros when there is no data.")
    public StatisticsService.Overview overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.overview(from, to);
    }

    @GetMapping("/by-category")
    @Operation(summary = "Spending or income split by category, highest first")
    public List<StatisticsService.CategoryBreakdown> byCategory(
            @RequestParam(required = false) com.finai.transaction.TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.byCategory(type, from, to);
    }

    @GetMapping("/monthly")
    @Operation(summary = "Twelve zero-filled monthly points for a year",
            description = "Used by the bar chart on the statistics screen.")
    public List<StatisticsService.MonthlyPoint> monthly(
            @RequestParam(required = false) Integer year) {
        return statisticsService.monthly(year == null ? LocalDate.now().getYear() : year);
    }

    @GetMapping("/daily")
    @Operation(summary = "Zero-filled daily income/expense for a window of at most 366 days")
    public List<StatisticsService.DailyPoint> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.daily(from, to);
    }
}
