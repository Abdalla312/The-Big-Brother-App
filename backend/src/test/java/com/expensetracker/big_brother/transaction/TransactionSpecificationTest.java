package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.common.TransactionType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TransactionSpecificationTest {

    private final Root<Transaction> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @Test
    void inMonth_NullInput_ReturnsConjunction() {
        TransactionSpecification.inMonth(null).toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void inMonth_BlankInput_ReturnsConjunction() {
        TransactionSpecification.inMonth("   ").toPredicate(root, query, cb);
        verify(cb).conjunction();
    }

    @Test
    void inMonth_YearMonthFormat_ParsesCorrectly() {
        Path datePath = mock(Path.class);
        when(root.get("transactionDate")).thenReturn(datePath);

        TransactionSpecification.inMonth("2026-09").toPredicate(root, query, cb);

        ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);

        verify(cb).between(eq(datePath), start.capture(), end.capture());
        assertThat(start.getValue()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(end.getValue()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void inMonth_MonthOnlyFormat_ParsesCurrentYear() {
        Path datePath = mock(Path.class);
        when(root.get("transactionDate")).thenReturn(datePath);

        TransactionSpecification.inMonth("09").toPredicate(root, query, cb);

        ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);
        verify(cb).between(eq(datePath), start.capture(), end.capture());
        YearMonth nine = YearMonth.of(YearMonth.now().getYear(), 9);
        assertThat(start.getValue()).isEqualTo(nine.atDay(1));
        assertThat(end.getValue()).isEqualTo(nine.atEndOfMonth());
    }

    @Test
    void inMonth_InvalidMonthValue_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> TransactionSpecification.inMonth("13"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid month format or value: 13");
    }

    @Test
    void inMonth_InvalidLength_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> TransactionSpecification.inMonth("2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Month format must be MM or yyyy-MM");
    }

    @Test
    void inMonth_InvalidDateValue_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> TransactionSpecification.inMonth("2026-13"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid month format or value: 2026-13");
    }

    @Test
    void inMonth_InvalidFormat_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> TransactionSpecification.inMonth("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Month format must be MM or yyyy-MM");
    }

    @Test
    void inDateRange_ValidRange_CreatesBetweenPredicate() {
        Path datePath = mock(Path.class);
        when(root.get("transactionDate")).thenReturn(datePath);
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        TransactionSpecification.inDateRange(from, to).toPredicate(root, query, cb);

        verify(root).get("transactionDate");
        verify(cb).between(datePath, from, to);
    }

    @Test
    void belongsToUser_CreatesEqualPredicate() {
        UUID userId = UUID.randomUUID();
        Path userPath = mock(Path.class);
        Path idPath = mock(Path.class);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.get("id")).thenReturn(idPath);

        TransactionSpecification.belongsToUser(userId).toPredicate(root, query, cb);

        verify(root).get("user");
        verify(userPath).get("id");
        verify(cb).equal(idPath, userId);
    }

    @Test
    void hasType_CreatesEqualPredicate() {
        Path typePath = mock(Path.class);
        when(root.get("type")).thenReturn(typePath);

        TransactionSpecification.hasType(TransactionType.EXPENSE).toPredicate(root, query, cb);

        verify(root).get("type");
        verify(cb).equal(typePath, TransactionType.EXPENSE);
    }

    @Test
    void hasCategory_CreatesEqualPredicate() {
        UUID categoryId = UUID.randomUUID();
        Path categoryPath = mock(Path.class);
        Path idPath = mock(Path.class);
        when(root.get("category")).thenReturn(categoryPath);
        when(categoryPath.get("id")).thenReturn(idPath);

        TransactionSpecification.hasCategory(categoryId).toPredicate(root, query, cb);

        verify(root).get("category");
        verify(cb).equal(idPath, categoryId);
    }

}
