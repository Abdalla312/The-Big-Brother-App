package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.BaseEntity;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

//   - `id` (UUID, Primary Key)
//   - `type` (Enum: INCOME, EXPENSE)
//   - `amount` (BigDecimal)
//   - `transactionDate` (LocalDate)
//   - `note` (String, Nullable)
//   - `paymentMethod` (String, Nullable)
//   - `user_id` (UUID, Foreign Key to User)
//   - `category_id` (UUID, Foreign Key to Category)
//   - `createdAt` (Timestamp)
//   - `updatedAt` (Timestamp)

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(length = 100)
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
