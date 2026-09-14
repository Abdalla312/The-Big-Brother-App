package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.transaction.dto.TransactionRequest;
import com.expensetracker.big_brother.transaction.dto.TransactionResponse;
import com.expensetracker.big_brother.transaction.dto.UpdateTransactionRequest;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = CategoryResponse.class)
public interface TransactionMapper {
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    Transaction toEntity(TransactionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Transaction partialUpdate(UpdateTransactionRequest updateTransactionRequest,
                              @MappingTarget Transaction transaction);
}
