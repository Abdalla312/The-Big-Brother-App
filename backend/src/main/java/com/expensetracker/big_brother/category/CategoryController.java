package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.category.dto.CreateCategoryRequest;
import com.expensetracker.big_brother.category.dto.UpdateCategoryRequest;
import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<CategoryResponse> categories = categoryService.getAllCategories(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(categories, "success"));
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
            @RequestBody UpdateCategoryRequest request,
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
}
