package com.expensetracker.big_brother.budget;

import com.expensetracker.big_brother.budget.dto.BudgetResponse;
import com.expensetracker.big_brother.category.CategoryMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = CategoryMapper.class)
public interface BudgetMapper {
    @Mapping(target = "spentAmount", ignore = true)
    @Mapping(target = "remainingAmount", ignore = true)
    @Mapping(target = "percentUsed", ignore = true)
    BudgetResponse toResponse(Budget budget);

    default BudgetResponse toResponseWithCalculations(Budget budget, BigDecimal spent) {
        BudgetResponse base = toResponse(budget);
        BigDecimal remaining = budget.getLimitAmount().subtract(spent);
        if (budget.getLimitAmount().compareTo(BigDecimal.ZERO) == 0){
            return new BudgetResponse(
                    base.id(), base.category(), base.month(), base.limitAmount(), spent, spent.negate(), 0.0, base.deletedAt());
        }
        double percent = spent.doubleValue() / budget.getLimitAmount().doubleValue() * 100.0;
        return new BudgetResponse(
                base.id(), base.category(), base.month(), base.limitAmount(),
                spent, remaining, Math.round(percent * 100.0) / 100.0, base.deletedAt());
    }

}
