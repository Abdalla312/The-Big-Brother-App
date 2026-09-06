package com.expensetracker.big_brother.budget;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.BaseEntity;
import com.expensetracker.big_brother.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

//        - `id` (UUID, Primary Key)
//        - `month` (YearMonth or String 'YYYY-MM')
//        - `limitAmount` (BigDecimal)
//        - `user_id` (UUID, Foreign Key to User)
//        - `category_id` (UUID, Foreign Key to Category)
//        - `createdAt` (Timestamp)
//        - `updatedAt` (Timestamp)
//        - *Constraint*: UNIQUE(`user_id`, `category_id`, `month`)

@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "category_id", "month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE budgets SET deleted_at = CURRENT_TIMESTAMP WHERE id=?")
@Filter(name = "deletedFilter", condition = "deleted_at IS NULL")
public class Budget extends BaseEntity {

    @Column(name = "month", nullable = false, length = 7)
    private String month;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal limitAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
