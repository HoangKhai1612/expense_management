package com.finai.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Per-category spending limits with derived usage")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "List the caller's active budgets with computed usage")
    public List<BudgetService.BudgetView> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return includeInactive ? budgetService.listAll() : budgetService.listActive();
    }

    @GetMapping("/current")
    @Operation(summary = "Return budgets for the current period and raise any newly crossed alert",
            description = "Used by the budget screen so a threshold crossing is detected on read, "
                    + "without depending on a background scheduler.")
    public List<BudgetService.BudgetView> current() {
        return budgetService.evaluateCurrentPeriodAndNotify();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Return one budget owned by the caller")
    public BudgetService.BudgetView get(@PathVariable Long id) {
        return budgetService.getOne(id);
    }

    @PostMapping
    @Operation(summary = "Create a budget for a category and period")
    public ResponseEntity<BudgetService.BudgetView> create(
            @RequestBody BudgetService.UpsertBudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a budget owned by the caller")
    public BudgetService.BudgetView update(
            @PathVariable Long id,
            @RequestBody BudgetService.UpsertBudgetRequest request) {
        return budgetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget owned by the caller")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
