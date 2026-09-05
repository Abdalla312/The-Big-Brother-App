package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.common.BypassSoftDelete;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findAllByUserIdOrUserIsNull(UUID userId, Pageable pageable);

    Page<Category> findAllByUserId(UUID userId, Pageable pageable);

    Page<Category> findAllByUserIdIsNull(Pageable pageable);

    @BypassSoftDelete
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.deletedAt IS NOT NULL ")
    Page<Category> findDeletedCategories(UUID userId, Pageable pageable);

    @BypassSoftDelete
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.deletedAt IS NOT NULL ")
    Optional<Category> findDeletedById(@Param("id") UUID id);
}
