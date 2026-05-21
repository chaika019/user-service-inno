package by.chaika19.userservice.service;

import by.chaika19.userservice.dto.UserRequestDto;
import by.chaika19.userservice.dto.UserResponseDto;
import by.chaika19.userservice.exception.BusinessException;
import by.chaika19.userservice.exception.ResourceNotFoundException;
import by.chaika19.userservice.mapper.UserMapper;
import by.chaika19.userservice.model.User;
import by.chaika19.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("Creating user")
    class CreatedUserTests {

        @Test
        @DisplayName("Create user success")
        void createUser_Success() {

            UserRequestDto requestDto = new UserRequestDto("Egor", "Chaika",
                    LocalDate.of(2000, 1, 1), "egor@gmail.com", true);
            UserResponseDto expectedDto =new UserResponseDto(1L, "Ivan", "Ivanov",
                    LocalDate.of(2000, 1, 1), "ivan@mail.com", true,
                    null, null, null);
            User user = new User();

            when(userRepository.findByEmail(requestDto.email())).thenReturn(Optional.empty());
            when(userMapper.toEntity(requestDto)).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toDto(user)).thenReturn(expectedDto);

            UserResponseDto actualResponse = userService.createUser(requestDto);

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.id()).isEqualTo(1L);
            assertThat(actualResponse.email()).isEqualTo("ivan@mail.com");

            verify(userRepository, times(1)).findByEmail(requestDto.email());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Error of creation. Email already in use")
        void createUser_ThrowsBusinessException_WhenEmailExists() {
            UserRequestDto request = new UserRequestDto("Ivan", "Ivanov", LocalDate.of(2000, 1, 1), "ivan@mail.com", true);
            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User with email ivan@mail.com already exists");

            verify(userRepository, never()).save(any());
        }

        @Nested
        @DisplayName("Find user by ID")
        class FindUserByIdTests {

            @Test
            @DisplayName("Find user by ID success")
            void findUserById_Success() {
                Long userId = 1L;
                User user = new User();
                UserResponseDto expectedDto = new UserResponseDto(userId, "Ivan", "Ivanov", null, "ivan@mail.com", true, null, null, null);

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(userMapper.toDto(user)).thenReturn(expectedDto);

                UserResponseDto actualDto = userService.findUserById(userId);

                assertThat(actualDto).isNotNull();
                assertThat(actualDto.id()).isEqualTo(userId);
            }

            @Test
            @DisplayName("Attempt to find user by ID, but throws an exception")
            void findUserById_ThrowsResourceNotFoundException() {
                Long userId = 10L;
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> userService.findUserById(userId))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("User not found with id: 10");
            }
        }
    }

    @Nested
    @DisplayName("Find all users with pagination and filtering")
    class FindAllUsersTests {

        @Test
        @DisplayName("Find all users success")
        @SuppressWarnings("unchecked")
        void findAllUsers_Success() {
            String name = "Ivan";
            String surname = "Ivanov";
            Pageable pageable = PageRequest.of(0, 10);

            User user = new User();
            Page<User> userPage = new PageImpl<>(List.of(user));
            UserResponseDto expectedDto = new UserResponseDto(1L, "Ivan", "Ivanov", null, "ivan@mail.com", true, null, null, null);

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toDto(user)).thenReturn(expectedDto);

            Page<UserResponseDto> actualPage = userService.findAllUsers(name, surname, pageable);

            assertThat(actualPage).isNotNull();
            assertThat(actualPage.getContent()).hasSize(1);
            assertThat(actualPage.getContent().get(0).id()).isEqualTo(1L);
            verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Updating user")
    class UpdateUserTests {

        @Test
        @DisplayName("Update user success")
        void updateUser_Success() {
            Long userId = 1L;
            UserRequestDto requestDto = new UserRequestDto("Egor", "Chaika", LocalDate.of(2000, 1, 1), "new@gmail.com", true);

            User existingUser = new User();
            existingUser.setId(userId);
            existingUser.setEmail("old@gmail.com");

            UserResponseDto expectedDto = new UserResponseDto(userId, "Egor", "Chaika", LocalDate.of(2000, 1, 1), "new@gmail.com", true, null, null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.findByEmail(requestDto.email())).thenReturn(Optional.empty());
            when(userMapper.toDto(existingUser)).thenReturn(expectedDto);

            UserResponseDto result = userService.updateUser(userId, requestDto);

            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("new@gmail.com");
            assertThat(existingUser.getName()).isEqualTo("Egor");
            assertThat(existingUser.getEmail()).isEqualTo("new@gmail.com");
        }

        @Test
        @DisplayName("Throws exception when updated email is already taken by another user")
        void updateUser_ThrowsBusinessException_WhenEmailAlreadyTaken() {
            Long userId = 1L;
            UserRequestDto requestDto = new UserRequestDto("Egor", "Chaika", LocalDate.of(2000, 1, 1), "taken@gmail.com", true);

            User existingUser = new User();
            existingUser.setId(userId);
            existingUser.setEmail("old@gmail.com");

            User anotherUser = new User();
            anotherUser.setId(2L);
            anotherUser.setEmail("taken@gmail.com");

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.findByEmail(requestDto.email())).thenReturn(Optional.of(anotherUser));

            assertThatThrownBy(() -> userService.updateUser(userId, requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email taken@gmail.com is already taken by another user");
        }
    }

    @Nested
    @DisplayName("Updating user active status")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Update status success")
        void updateUserStatus_Success() {
            Long userId = 1L;
            User user = new User();
            user.setId(userId);
            user.setActive(true);

            UserResponseDto expectedDto = new UserResponseDto(userId, "Ivan", "Ivanov", null, "ivan@mail.com", false, null, null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(expectedDto);

            UserResponseDto result = userService.updateUserStatus(userId, false);

            assertThat(result).isNotNull();
            assertThat(result.active()).isFalse();
            assertThat(user.getActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Deleting user")
    class DeleteUserTests {

        @Test
        @DisplayName("Delete user success")
        void deleteUser_Success() {
            Long userId = 1L;
            User user = new User();
            user.setId(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            userService.deleteUser(userId);

            verify(userRepository, times(1)).deleteById(userId);
        }
    }
}