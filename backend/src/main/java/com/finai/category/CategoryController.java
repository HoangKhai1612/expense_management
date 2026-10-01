package com.finai.category;

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
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "System catalogue and the caller's personal categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List categories available to the caller",
            description = "Returns the active system categories plus the caller's own personal ones.")
    public List<CategoryService.CategoryView> list(
            @RequestParam(required = false) CategoryType type) {
        return categoryService.listForCurrentUser(type);
    }

    @PostMapping
    @Operation(summary = "Create a personal category")
    public ResponseEntity<CategoryService.CategoryView> create(
            @RequestBody CategoryService.UpsertPersonalCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createPersonalCategory(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of the caller's personal categories")
    public CategoryService.CategoryView update(
            @PathVariable Long id,
            @RequestBody CategoryService.UpsertPersonalCategoryRequest request) {
        return categoryService.updatePersonalCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate one of the caller's personal categories",
            description = "Soft delete: the category is hidden from pickers but keeps its history.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deactivatePersonalCategory(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
