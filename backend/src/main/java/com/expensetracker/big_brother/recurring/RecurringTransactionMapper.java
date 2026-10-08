package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.recurring.dto.CreateRecurringTransactionRequest;
import com.expensetracker.big_brother.recurring.dto.RecurringTransactionResponse;
import com.expensetracker.big_brother.recurring.dto.UpdateRecurringTransactionRequest;
import org.mapstruct.*;

import java.time.LocalDate;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecurringTransactionMapper {
    @Mapping(source = "startDate", target = "nextExecutionDate")
    @Mapping(source = "startDate", target = "scheduledDayOfMonth", qualifiedByName = "dayOfMonth")
    RecurringTransaction toEntity(CreateRecurringTransactionRequest createRecurringTransactionRequest);

    @Named("dayOfMonth")
    static int dayOfMonth(LocalDate date) {
        return date.getDayOfMonth();
    }

    @Mapping(source = "active", target = "isActive")
    RecurringTransactionResponse toDto(RecurringTransaction recurringTransaction);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    RecurringTransaction partialUpdate(UpdateRecurringTransactionRequest updateRecurringTransactionRequest,
                                       @MappingTarget RecurringTransaction recurringTransaction);

    @AfterMapping
    default void updateScheduledDay(
            UpdateRecurringTransactionRequest request,
            @MappingTarget RecurringTransaction entity) {
        if (request.nextExecutionDate() != null) {
            entity.setScheduledDayOfMonth(request.nextExecutionDate().getDayOfMonth());
        }
    }
}