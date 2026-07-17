package com.expensetracker.big_brother.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findAllByUserIdOrUserIsNull(UUID userId, Pageable pageable);

    Page<Category> findAllByUserId(UUID userId, Pageable pageable);

    Page<Category> findAllByUserIdIsNull(Pageable pageable);
}
