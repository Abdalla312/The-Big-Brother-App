package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.recurring.dto.CreateRecurringTransactionRequest;
import com.expensetracker.big_brother.recurring.dto.RecurringTransactionResponse;
import com.expensetracker.big_brother.recurring.dto.UpdateRecurringTransactionRequest;
import com.expensetracker.big_brother.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecurringTransactionResponse>>> getAll(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 20, sort = "nextExecutionDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getAllRecurringTransactions(user.getUserId(), pageable), "Retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> getItem(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getRecurringTransaction(user.getUserId(), id), "Retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateRecurringTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        service.createRecurringTransaction(request, user.getUserId()), "Created successfully"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringTransactionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.updateRecurringTransaction(id, request, user.getUserId()), "Updated"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> toggleActive(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.toggleActive(id, user.getUserId()), "Status toggled"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.deleteRecurringTransaction(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Deleted successfully"));
    }
}
