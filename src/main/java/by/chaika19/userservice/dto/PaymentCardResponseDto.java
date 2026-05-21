package by.chaika19.userservice.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PaymentCardResponseDto(
        Long id,
        String number,
        String holder,
        LocalDate expirationDate,
        Boolean active,
        Long userId,
        Instant createdAt,
        Instant updatedAt
) { }
