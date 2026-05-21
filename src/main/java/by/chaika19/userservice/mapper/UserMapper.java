package by.chaika19.userservice.mapper;

import by.chaika19.userservice.dto.UserRequestDto;
import by.chaika19.userservice.dto.UserResponseDto;
import by.chaika19.userservice.model.User;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = PaymentCardMapper.class,
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface UserMapper{

    UserResponseDto toDto(User user);

    User toEntity(UserResponseDto userDto);

    User toEntity(UserRequestDto userRequestDto);
}
