package by.chaika19.userservice.mapper;

import by.chaika19.userservice.dto.PaymentCardRequestDto;
import by.chaika19.userservice.dto.PaymentCardResponseDto;
import by.chaika19.userservice.model.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface PaymentCardMapper {

    @Mapping(source = "user.id", target = "userId")
    PaymentCardResponseDto toDto(PaymentCard paymentCard);

    @Mapping(source = "userId", target = "user.id")
    PaymentCard toEntity(PaymentCardResponseDto paymentCardDto);

    @Mapping(source = "userId", target = "user.id")
    PaymentCard toEntity(PaymentCardRequestDto paymentCardRequestDto);
}
