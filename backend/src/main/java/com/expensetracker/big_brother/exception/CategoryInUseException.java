package com.expensetracker.big_brother.exception;

public class CategoryInUseException extends RuntimeException {
    public CategoryInUseException() {
        super("Cannot delete category. It is currently used by existing transactions.");
    }
}
