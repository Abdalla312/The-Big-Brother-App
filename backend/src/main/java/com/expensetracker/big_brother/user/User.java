package com.expensetracker.big_brother.user;

import com.expensetracker.big_brother.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

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
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id=?")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private int tokenVersion = 0;

    @Column(nullable = false )
    private boolean userVerified = false;



}
