package by.chaika19.userservice.controller;

import by.chaika19.userservice.BaseIntegrationTest;
import by.chaika19.userservice.dto.UserRequestDto;
import by.chaika19.userservice.dto.UserResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Full User lifecycle: Create -> Read (Caches) -> Update (Evicts) -> Delete")
    void userLifecycleFlow() {
        UserRequestDto request = new UserRequestDto(
                "Jane", "Doe", LocalDate.of(1995, 10, 15), "jane.doe@mail.com", true
        );
        ResponseEntity<UserResponseDto> createResponse = restTemplate.postForEntity(
                "/api/users", request, UserResponseDto.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponseDto created = createResponse.getBody();
        assertThat(created).isNotNull();
        Long userId = created.id();

        assertThat(Objects.requireNonNull(cacheManager.getCache("users")).get(userId)).isNull();

        ResponseEntity<UserResponseDto> getResponse = restTemplate.getForEntity(
                "/api/users/" + userId, UserResponseDto.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        UserResponseDto cachedUser = Objects.requireNonNull(cacheManager.getCache("users"))
                .get(userId, UserResponseDto.class);
        assertThat(cachedUser).isNotNull();
        assertThat(cachedUser.name()).isEqualTo("Jane");

        UserRequestDto updateRequest = new UserRequestDto(
                "Janet", "Doe", LocalDate.of(1995, 10, 15), "jane.doe@mail.com", true
        );
        ResponseEntity<UserResponseDto> updateResponse = restTemplate.exchange(
                "/api/users/" + userId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                UserResponseDto.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(Objects.requireNonNull(cacheManager.getCache("users")).get(userId)).isNull();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/users/" + userId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(userRepository.findById(userId)).isEmpty();
    }
}