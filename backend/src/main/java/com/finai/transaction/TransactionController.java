package com.finai.transaction;

import com.finai.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Income and expense entries owned by the caller")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "List the caller's transactions",
            description = "Filter by type, category and date range. Only the caller's own rows are returned.")
    public PageResponse<TransactionService.TransactionView> list(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionService.search(type, categoryId, from, to, page, size);
    }

    @GetMapping("/recent")
    @Operation(summary = "Return the five most recent transactions for the home screen")
    public List<TransactionService.TransactionView> recent() {
        return transactionService.recent();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Return one transaction owned by the caller")
    public TransactionService.TransactionView get(@PathVariable Long id) {
        return transactionService.getOne(id);
    }

    @PostMapping
    @Operation(summary = "Record a new income or expense entry")
    public ResponseEntity<TransactionService.TransactionView> create(
            @RequestBody TransactionService.UpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a transaction owned by the caller")
    public TransactionService.TransactionView update(
            @PathVariable Long id,
            @RequestBody TransactionService.UpsertRequest request) {
        return transactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction owned by the caller")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
