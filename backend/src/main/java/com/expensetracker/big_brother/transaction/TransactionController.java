package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.transaction.dto.TransactionExportFilter;
import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.transaction.dto.TransactionRequest;
import com.expensetracker.big_brother.transaction.dto.TransactionResponse;
import com.expensetracker.big_brother.transaction.dto.UpdateTransactionRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    // list all transactions related to user
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
        PageResponse<TransactionResponse> response = transactionService.getTransactions(user.getUserId(), month, categoryId, type, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response, "Transactions retrieved"));
    }

    // get certain transaction owned by user
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user) {
        TransactionResponse response = transactionService.getTransaction(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Transaction retrieved"));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public void exportTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID categoryId,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"transactions-" + timestamp + ".csv\"");

        TransactionExportFilter filter = new TransactionExportFilter(from, to, type, categoryId);
        transactionService.exportTransactionsCsv(user.getUserId(),
                filter,
                response.getOutputStream());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        TransactionResponse response = transactionService.createTransaction(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Transaction successfully created"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        TransactionResponse response = transactionService.updateTransaction(id, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Transaction updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails user) {
        transactionService.deleteTransaction(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Transaction deleted successfully"));
    }
}
