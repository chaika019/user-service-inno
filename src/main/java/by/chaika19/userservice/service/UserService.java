package by.chaika19.userservice.service;

import by.chaika19.userservice.dto.UserRequestDto;
import by.chaika19.userservice.dto.UserResponseDto;
import by.chaika19.userservice.exception.BusinessException;
import by.chaika19.userservice.exception.ResourceNotFoundException;
import by.chaika19.userservice.mapper.UserMapper;
import by.chaika19.userservice.model.User;
import by.chaika19.userservice.repository.UserRepository;
import by.chaika19.userservice.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto createUser(UserRequestDto userRequestDto) {

        if (userRepository.existsByEmail(userRequestDto.email())) {
            throw new BusinessException("User with email " + userRequestDto.email() + " already exists");
        }

        User user = userMapper.toEntity(userRequestDto);
        return userMapper.toDto(userRepository.save(user));
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponseDto findUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public Page<UserResponseDto> findAllUsers(String name, String surname, Pageable pageable) {
        Specification<User> specification = Specification.where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));

        return userRepository.findAll(specification, pageable)
                .map(userMapper::toDto);
    }

    /**
     * Updates user name, surname, birthdate, email.
     * Note: The 'active' status from UserRequestDto is explicitly ignored here.
     * User status transitions must be handled solely via {@link #updateUserStatus(Long, Boolean)}.
     */
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDto updateUser(Long id, UserRequestDto userRequestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equals(userRequestDto.email()) &&
                userRepository.existsByEmail(userRequestDto.email())) {
            throw new BusinessException("Email " + userRequestDto.email() + " is already taken by another user");
        }

        user.setName(userRequestDto.name());
        user.setSurname(userRequestDto.surname());
        user.setBirthDate(userRequestDto.birthDate());
        user.setEmail(userRequestDto.email());
        return userMapper.toDto(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDto updateUserStatus(Long id, Boolean active) {
        userRepository.updateUserStatus(id, active);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        return userMapper.toDto(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        userRepository.deleteById(id);
    }
}
