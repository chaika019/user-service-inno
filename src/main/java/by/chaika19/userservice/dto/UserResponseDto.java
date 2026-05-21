package by.chaika19.userservice.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record UserResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        Boolean active,
        List<PaymentCardResponseDto> cards,
        Instant createdAt,
        Instant updatedAt
) { }
