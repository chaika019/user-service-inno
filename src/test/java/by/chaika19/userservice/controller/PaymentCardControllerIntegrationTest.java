package by.chaika19.userservice.controller;

import by.chaika19.userservice.BaseIntegrationTest;
import by.chaika19.userservice.dto.PaymentCardRequestDto;
import by.chaika19.userservice.dto.PaymentCardResponseDto;
import by.chaika19.userservice.dto.UserRequestDto;
import by.chaika19.userservice.dto.UserResponseDto;
import by.chaika19.userservice.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCardControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Create cards, verify max limit of 5 cards, and verify cache eviction on card deletion")
    void cardCreationFlow_andLimitVerification() {
        UserRequestDto userRequest = new UserRequestDto(
                "Egor", "Chaika", LocalDate.of(2000, 1, 1), "egor.card@mail.com", true
        );
        UserResponseDto user = restTemplate.postForEntity("/api/users", userRequest, UserResponseDto.class).getBody();
        assertThat(user).isNotNull();
        Long userId = user.id();

        Long cardIdToDelete = null;

        for (int i = 1; i <= 5; i++) {
            PaymentCardRequestDto cardRequest = new PaymentCardRequestDto(
                    String.format("%016d", i), "Egor Chaika", LocalDate.now().plusYears(3), true, userId
            );
            ResponseEntity<PaymentCardResponseDto> cardResponse = restTemplate.postForEntity(
                    "/api/payment-cards", cardRequest, PaymentCardResponseDto.class
            );
            assertThat(cardResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            if (i == 1) {
                cardIdToDelete = Objects.requireNonNull(cardResponse.getBody()).id();
            }
        }

        PaymentCardRequestDto cardRequest6 = new PaymentCardRequestDto(
                "9999999999999999", "Egor Chaika", LocalDate.now().plusYears(3), true, userId
        );
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> errorResponse = restTemplate.postForEntity(
                "/api/payment-cards", cardRequest6, GlobalExceptionHandler.ErrorResponse.class
        );

        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(errorResponse.getBody()).message())
                .contains("User already has maximum number of cards 5");

        restTemplate.getForEntity("/api/users/" + userId, UserResponseDto.class);
        assertThat(Objects.requireNonNull(cacheManager.getCache("users")).get(userId)).isNotNull();

        ResponseEntity<Void> deleteCardResponse = restTemplate.exchange(
                "/api/payment-cards/" + cardIdToDelete,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteCardResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(Objects.requireNonNull(cacheManager.getCache("users")).get(userId)).isNull();
    }
}