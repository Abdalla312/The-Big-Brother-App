package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.recurring.dto.CreateRecurringTransactionRequest;
import com.expensetracker.big_brother.recurring.dto.RecurringTransactionResponse;
import com.expensetracker.big_brother.recurring.dto.UpdateRecurringTransactionRequest;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecurringTransactionMapper {
    @Mapping(source = "startDate", target = "nextExecutionDate")
    RecurringTransaction toEntity(CreateRecurringTransactionRequest createRecurringTransactionRequest);

    @Mapping(source = "active", target = "isActive")
    RecurringTransactionResponse toDto(RecurringTransaction recurringTransaction);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    RecurringTransaction partialUpdate(UpdateRecurringTransactionRequest updateRecurringTransactionRequest,
                                       @MappingTarget RecurringTransaction recurringTransaction);
}