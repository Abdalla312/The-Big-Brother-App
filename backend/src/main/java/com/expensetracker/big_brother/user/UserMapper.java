package com.expensetracker.big_brother.user;

import com.expensetracker.big_brother.user.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
