package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.category.dto.CreateCategoryRequest;
import com.expensetracker.big_brother.category.dto.UpdateCategoryRequest;
import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAll(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 50, sort = "name") Pageable pageable,
            @RequestParam(defaultValue = "user") String type) {
        PageResponse<CategoryResponse> categories = categoryService.getAllCategories(user.getUserId(), type, pageable);
        return ResponseEntity.ok(ApiResponse.ok(categories, "Categories retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @RequestBody @Valid CreateCategoryRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        CategoryResponse response = categoryService.createCategory(request, user.getUserId());
        return ResponseEntity.created(URI.create("/api/v1/categories"))
                .body(ApiResponse.created(response, "Category created"));

    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateCategoryRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        CategoryResponse response = categoryService.updateCategory(id, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Category updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user) {
        categoryService.deleteCategory(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deleted"));
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategoriesTrash(@AuthenticationPrincipal CustomUserDetails user, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(categoryService.getDeletedCategories(user.getUserId(), pageable)), "Categories trash retrieved"));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<CategoryResponse>> restoreDeletedCategory(@AuthenticationPrincipal CustomUserDetails user, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.restoreDeletedCategory(user.getUserId(), id), "Category restored"));
    }
}
