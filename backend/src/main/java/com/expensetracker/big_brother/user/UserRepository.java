package com.expensetracker.big_brother.user;

import com.expensetracker.big_brother.common.BypassSoftDelete;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Override
    @Query("SELECT u FROM User u WHERE u.id = :id ")
    @NotNull
    Optional<User> findById(@NotNull UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @BypassSoftDelete
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL ")
    Page<User> findDeletedUsers(Pageable pageable);

    @BypassSoftDelete
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findDeletedById(@Param("id") UUID id);
}
