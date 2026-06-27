package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.common.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public class TransactionSpecification {
    public static Specification<Transaction> belongsToUser(UUID userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasCategory(UUID categoryId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Transaction> inMonth(String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("transactionDate"), start, end);
    }

    public static Specification<Transaction> inDateRange(LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("transactionDate"), from, to);
    }

}
