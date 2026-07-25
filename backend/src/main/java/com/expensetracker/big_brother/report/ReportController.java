package com.expensetracker.big_brother.report;

import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.report.dto.*;
import com.expensetracker.big_brother.security.CustomUserDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate endDate) {
        MonthlySummaryResponse response = reportService.getMonthlySummary(principal.getUserId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(response, "Monthly Summary retrieved"));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<ApiResponse<List<CategoryBreakdownResponse>>> getCategoryBreakdown(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type) {
        List<CategoryBreakdownResponse> responseList = reportService.getCategoryBreakdown(
                principal.getUserId(), startDate, endDate,
                (type != null) ? type : TransactionType.EXPENSE);
        return ResponseEntity.ok(ApiResponse.ok(responseList, "Category breakdown retrieved"));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<TrendResponse>>> getTrend(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate endDate) {

        List<TrendResponse> responseList = reportService.getTrend(principal.getUserId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(responseList, "Trend retrieved"));
    }

    @GetMapping("/budget-comparison")
    public ResponseEntity<ApiResponse<List<BudgetComparisonResponse>>> getBudgetComparison(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam @NotBlank String month) {
        List<BudgetComparisonResponse> responseList = reportService.getBudgetComparison(principal.getUserId(), month);
        return ResponseEntity.ok(ApiResponse.ok(responseList, "Budget comparison retrieved"));
    }

    @GetMapping("/payment-method-breakdown")
    public ResponseEntity<ApiResponse<List<PaymentBreakdown>>> getPaymentMethodBreakdown(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DATE) LocalDate endDate,
            @RequestParam(defaultValue = "EXPENSE") TransactionType type) {
        List<PaymentBreakdown> response = reportService.paymentMethodBreakdowns(principal.getUserId(), startDate, endDate, type);
        return ResponseEntity.ok(ApiResponse.ok(response, "Payment method breakdown retrieved"));
    }

}
