package com.expensetracker.big_brother.budget;

import com.expensetracker.big_brother.budget.dto.BudgetRequest;
import com.expensetracker.big_brother.budget.dto.BudgetResponse;
import com.expensetracker.big_brother.budget.dto.UpdateBudgetRequest;
import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/budget")
public class BudgetController {
    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BudgetResponse>>> getBudgets(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String month,
            @PageableDefault(size = 20)Pageable pageable) {
        PageResponse<BudgetResponse> response = budgetService.getBudgets(user.getUserId(), month, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Budgets retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        BudgetResponse response = budgetService.createBudget(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Budget created"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateBudgetRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        BudgetResponse response = budgetService.updateBudget(id, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Budget updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user) {
        budgetService.deleteBudget(user.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Budget deleted"));
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<PageResponse<BudgetResponse>>> getDeletedBudgets(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(page = 20, sort = "deletedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(budgetService.getDeletedBudgets(user.getUserId(), pageable)), "Budget trash retrieved"));
    }

    @PutMapping("{id}/restore")
    public ResponseEntity<ApiResponse<BudgetResponse>> restoreDeletedBudget(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.restoreDeletedBudget(user.getUserId(), id), "Budget restored"));
    }
}
