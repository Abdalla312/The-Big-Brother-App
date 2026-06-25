package com.expensetracker.big_brother.exception;

public class ResourceOwnershipException extends RuntimeException {
    public ResourceOwnershipException() {
        super("You do not have permission to access this resource");
    }
}
