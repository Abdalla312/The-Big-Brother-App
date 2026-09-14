package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.common.BypassSoftDelete;
import com.expensetracker.big_brother.common.TransactionType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Override
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    @NotNull
    Optional<Category> findById(@NotNull UUID id);

    @BypassSoftDelete
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.deletedAt IS NOT NULL ")
    Page<Category> findDeletedCategories(UUID userId, Pageable pageable);

    @BypassSoftDelete
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.deletedAt IS NOT NULL ")
    Optional<Category> findDeletedById(@Param("id") UUID id);

    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND (:type IS NULL OR c.type = :type)")
    Page<Category> findUserCategories(@Param("userId") UUID userId, @Param("type") TransactionType type, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.user.id IS NULL AND (:type IS NULL OR c.type = : type)")
    Page<Category> findDefaultCategories(@Param("type") TransactionType type, Pageable pageable);
}
