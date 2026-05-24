package com.expensetracker.big_brother.user;

import com.expensetracker.big_brother.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//   - `id` (UUID, Primary Key)
//   - `name` (String)
//   - `email` (String, Unique)
//   - `passwordHash` (String)
//   - `role` (Enum: USER, ADMIN)
//   - `createdAt` (Timestamp)
//   - `updatedAt` (Timestamp)
@Entity
@Table(name = "users")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;


}
