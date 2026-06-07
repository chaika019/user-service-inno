package by.chaika19.userservice.controller;

import by.chaika19.userservice.BaseIntegrationTest;
import by.chaika19.userservice.dto.UserRequestDto;
import by.chaika19.userservice.dto.UserResponseDto;
import by.chaika19.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
        Objects.requireNonNull(cacheManager.getCache("users")).clear();
    }

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

    @Test
    @DisplayName("Find all users with filtering and pagination")
    void findAllUsers_WithFilteringAndPagination_ShouldReturnPagedList() {
        User user1 = new User();
        user1.setName("Egor");
        user1.setSurname("Chaika");
        user1.setEmail("egor@gmail.com");
        user1.setBirthDate(LocalDate.of(2000, 1, 1));
        user1.setActive(true);
        userRepository.save(user1);

        String url = UriComponentsBuilder.fromUriString("/api/users")
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

        assertThat(content.get(0).path("name").asString()).isEqualTo("Egor");
        assertThat(content.get(0).path("email").asString()).isEqualTo("egor@gmail.com");
    }

    @Test
    @DisplayName("Update user active status (activate/deactivate)")
    void updateUserStatus_ShouldToggleActiveField() {
        User user = new User();
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setEmail("ivan@mail.com");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setActive(true);
        user = userRepository.save(user);
        Long userId = user.getId();

        String deactivateUrl = UriComponentsBuilder.fromUriString("/api/users/{id}/status/{active}")
                .buildAndExpand(userId, false)
                .toUriString();

        ResponseEntity<Void> deactivateResponse = restTemplate.exchange(
                deactivateUrl,
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(deactivateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        User deactivatedUserInDb = userRepository.findById(userId).orElseThrow();
        assertThat(deactivatedUserInDb.getActive()).isFalse();

        String activateUrl = UriComponentsBuilder.fromUriString("/api/users/{id}/status/{active}")
                .buildAndExpand(userId, true)
                .toUriString();

        ResponseEntity<Void> activateResponse = restTemplate.exchange(
                activateUrl,
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(activateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        User activatedUserInDb = userRepository.findById(userId).orElseThrow();
        assertThat(activatedUserInDb.getActive()).isTrue();
    }
}