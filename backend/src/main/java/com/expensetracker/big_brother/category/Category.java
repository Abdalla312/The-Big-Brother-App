package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.common.BaseEntity;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

//   - `id` (UUID, Primary Key)
//   - `name` (String)
//   - `type` (Enum: INCOME, EXPENSE)
//   - `color` (String - Hex Code)
//   - `icon` (String - Icon Name)
//   - `user_id` (UUID, Foreign Key to User. Nullable for system-wide defaults)
//   - `createdAt` (Timestamp)
//   - `updatedAt` (Timestamp)
//
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE categories SET deleted_at = CURRENT_TIMESTAMP WHERE id=?")
public class Category extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionType type;

    @Column(length = 20)
    private String color;

    @Column(length = 50)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
