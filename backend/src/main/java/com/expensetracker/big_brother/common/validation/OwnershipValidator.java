package com.expensetracker.big_brother.common.validation;

import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OwnershipValidator {
    public void validateOwnership(UUID resourceOwnerId, UUID currentUserId) {
        if (!resourceOwnerId.equals(currentUserId)) {
            throw new ResourceOwnershipException();
        }
    }
}
