package by.chaika19.userservice.controller;

import by.chaika19.userservice.BaseIntegrationTest;
import by.chaika19.userservice.dto.PaymentCardRequestDto;
import by.chaika19.userservice.dto.PaymentCardResponseDto;
import by.chaika19.userservice.dto.UserRequestDto;
import by.chaika19.userservice.dto.UserResponseDto;
import by.chaika19.userservice.exception.GlobalExceptionHandler;
import by.chaika19.userservice.model.PaymentCard;
import by.chaika19.userservice.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

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

    @Test
    @DisplayName("Find all cards with filtering and pagination")
    void findAllCards_WithFilteringAndPagination_ShouldReturnPagedList() {
        User user = new User();
        user.setName("Egor");
        user.setSurname("Chaika");
        user.setEmail("egor.filter@mail.com");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setActive(true);
        user = userRepository.save(user);

        PaymentCard card = new PaymentCard();
        card.setNumber("1111222233334444");
        card.setHolder("Egor Chaika");
        card.setExpirationDate(LocalDate.now().plusYears(2));
        card.setActive(true);
        card.setUser(user);
        paymentCardRepository.save(card);

        String url = UriComponentsBuilder.fromUriString("/api/payment-cards")
                .queryParam("name", "Egor")
                .queryParam("surname", "Chaika")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .toUriString();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = response.getBody();
        assertThat(root).isNotNull();

        JsonNode content = root.path("content");
        long totalElements = root.path("totalElements").asLong();

        assertThat(content.isArray()).isTrue();
        assertThat(content).hasSize(1);
        assertThat(totalElements).isEqualTo(1);

        assertThat(content.get(0).path("number").asString()).isEqualTo("1111222233334444");
        assertThat(content.get(0).path("holder").asString()).isEqualTo("Egor Chaika");
    }

    @Test
    @DisplayName("Update card active status (activate/deactivate)")
    void updateCardStatus_ShouldToggleActiveField() {
        User user = new User();
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setEmail("ivan.card@mail.com");
        user.setBirthDate(LocalDate.of(1995, 5, 10));
        user.setActive(true);
        user = userRepository.save(user);

        PaymentCard card = new PaymentCard();
        card.setNumber("5555666677778888");
        card.setHolder("Ivan Ivanov");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setActive(true);
        card.setUser(user);
        card = paymentCardRepository.save(card);
        Long cardId = card.getId();

        String deactivateUrl = UriComponentsBuilder.fromUriString("/api/payment-cards/{id}/status/{active}")
                .buildAndExpand(cardId, false)
                .toUriString();

        ResponseEntity<Void> deactivateResponse = restTemplate.exchange(
                deactivateUrl,
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(deactivateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        PaymentCard deactivatedCardInDb = paymentCardRepository.findById(cardId).orElseThrow();
        assertThat(deactivatedCardInDb.getActive()).isFalse();

        String activateUrl = UriComponentsBuilder.fromUriString("/api/payment-cards/{id}/status/{active}")
                .buildAndExpand(cardId, true)
                .toUriString();

        ResponseEntity<Void> activateResponse = restTemplate.exchange(
                activateUrl,
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(activateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        PaymentCard activatedCardInDb = paymentCardRepository.findById(cardId).orElseThrow();
        assertThat(activatedCardInDb.getActive()).isTrue();
    }
}