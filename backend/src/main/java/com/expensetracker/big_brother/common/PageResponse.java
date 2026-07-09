package com.expensetracker.big_brother.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        int totalPages,
        Long totalElements,
        boolean first,
        boolean last,
        boolean empty,
        int numberOfElements,
        List<SortInfo> sort
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        Sort sortObj = page.getSort();
        List<SortInfo> sortInfo = sortObj.isSorted()
                ? sortObj.stream().map(
                o -> new SortInfo(o.getProperty(), o.getDirection().name())).toList()
                : List.of();
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                page.getNumberOfElements(),
                sortInfo
        );
    }

    public record SortInfo(String property, String direction) {
    }
}
