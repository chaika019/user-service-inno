package by.chaika19.userservice.service;

import by.chaika19.userservice.dto.PaymentCardRequestDto;
import by.chaika19.userservice.dto.PaymentCardResponseDto;
import by.chaika19.userservice.exception.BusinessException;
import by.chaika19.userservice.exception.ResourceNotFoundException;
import by.chaika19.userservice.mapper.PaymentCardMapper;
import by.chaika19.userservice.model.PaymentCard;
import by.chaika19.userservice.model.User;
import by.chaika19.userservice.repository.PaymentCardRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private PaymentCardService paymentCardService;

    @Nested
    @DisplayName("Creating payment card")
    class CreateCardTests {

        @Test
        @DisplayName("Create card success when user has less than 5 cards")
        void createCard_Success() {
            Long userId = 1L;
            PaymentCardRequestDto requestDto = new PaymentCardRequestDto(
                    "1234567812345678", "Egor Chaika", LocalDate.of(2030, 12, 31), true, userId
            );

            User user = new User();
            user.setId(userId);
            user.setCards(new ArrayList<>());

            PaymentCard paymentCard = new PaymentCard();
            PaymentCardResponseDto expectedResponse = new PaymentCardResponseDto(
                    10L, "1234567812345678", "Egor Chaika", LocalDate.of(2030, 12, 31), true, userId, null, null
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(paymentCardMapper.toEntity(requestDto)).thenReturn(paymentCard);
            when(paymentCardRepository.save(paymentCard)).thenReturn(paymentCard);
            when(paymentCardMapper.toDto(paymentCard)).thenReturn(expectedResponse);

            PaymentCardResponseDto actualResponse = paymentCardService.create(requestDto);

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.id()).isEqualTo(10L);
            assertThat(actualResponse.userId()).isEqualTo(userId);
            verify(paymentCardRepository, times(1)).save(paymentCard);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when user does not exist")
        void createCard_ThrowsResourceNotFoundException_WhenUserNotFound() {
            Long userId = 999L;
            PaymentCardRequestDto requestDto = new PaymentCardRequestDto(
                    "1234567812345678", "Egor Chaika", LocalDate.of(2030, 12, 31), true, userId
            );

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentCardService.create(requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }

        @Test
        @DisplayName("Throw BusinessException when user already has 5 or more cards")
        void createCard_ThrowsBusinessException_WhenCardLimitReached() {
            Long userId = 1L;
            PaymentCardRequestDto requestDto = new PaymentCardRequestDto(
                    "1234567812345678", "Egor Chaika", LocalDate.of(2030, 12, 31), true, userId
            );

            User user = new User();
            user.setId(userId);

            List<PaymentCard> existingCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                existingCards.add(new PaymentCard());
            }
            user.setCards(existingCards);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> paymentCardService.create(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User already has maximum number of cards 5");

            verify(paymentCardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find card by ID")
    class FindCardByIdTests {

        @Test
        @DisplayName("Find existing card success")
        void findById_Success() {
            Long cardId = 10L;
            PaymentCard card = new PaymentCard();
            PaymentCardResponseDto expectedResponse = new PaymentCardResponseDto(
                    cardId, "1234567812345678", "Egor Chaika", LocalDate.of(2030, 12, 31), true, 1L, null, null
            );

            when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));
            when(paymentCardMapper.toDto(card)).thenReturn(expectedResponse);

            PaymentCardResponseDto result = paymentCardService.findById(cardId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(cardId);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when card not found")
        void findById_ThrowsResourceNotFoundException_WhenCardNotFound() {
            Long cardId = 999L;
            when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentCardService.findById(cardId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Card not found with id: 999");
        }
    }

    @Nested
    @DisplayName("Find all cards with filtering and pagination")
    class FindAllCardsTests {

        @Test
        @DisplayName("Find all cards success")
        @SuppressWarnings("unchecked")
        void findAllCards_Success() {
            String name = "Ivan";
            String surname = "Ivanov";
            Pageable pageable = PageRequest.of(0, 10);

            PaymentCard card = new PaymentCard();
            Page<PaymentCard> cardPage = new PageImpl<>(List.of(card));
            PaymentCardResponseDto expectedDto = new PaymentCardResponseDto(
                    10L, "1234567812345678", "Egor", null, true, 1L, null, null
            );

            when(paymentCardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);
            when(paymentCardMapper.toDto(card)).thenReturn(expectedDto);

            Page<PaymentCardResponseDto> result = paymentCardService.findAllCards(name, surname, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(paymentCardRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Find all cards by User ID")
    class FindAllByUserIdTests {

        @Test
        @DisplayName("Find all cards by user ID success")
        void findAllByUserId_Success() {
            Long userId = 1L;
            PaymentCard card = new PaymentCard();
            PaymentCardResponseDto expectedDto = new PaymentCardResponseDto(
                    10L, "1234567812345678", "Egor", null, true, userId, null, null
            );

            when(paymentCardRepository.findAllByUserId(userId)).thenReturn(List.of(card));
            when(paymentCardMapper.toDto(card)).thenReturn(expectedDto);

            List<PaymentCardResponseDto> result = paymentCardService.findAllByUserId(userId);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).userId()).isEqualTo(userId);
            verify(paymentCardRepository, times(1)).findAllByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Updating card details")
    class UpdateCardTests {

        @Test
        @DisplayName("Update card details success when number is unchanged")
        void updateCard_Success_WhenNumberUnchanged() {
            Long cardId = 10L;
            PaymentCardRequestDto requestDto = new PaymentCardRequestDto(
                    "1234567812345678", "New Holder", LocalDate.of(2035, 1, 1), true, 1L
            );

            PaymentCard existingCard = new PaymentCard();
            existingCard.setId(cardId);
            existingCard.setNumber("1234567812345678");

            PaymentCardResponseDto expectedResponse = new PaymentCardResponseDto(
                    cardId, "1234567812345678", "New Holder", LocalDate.of(2035, 1, 1), true, 1L, null, null
            );

            when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
            when(paymentCardMapper.toDto(existingCard)).thenReturn(expectedResponse);

            PaymentCardResponseDto result = paymentCardService.updateCard(cardId, requestDto);

            assertThat(result).isNotNull();
            assertThat(existingCard.getHolder()).isEqualTo("New Holder");
            verify(paymentCardRepository, never()).existsByNumber(anyString());
        }

        @Test
        @DisplayName("Update card details success when number is changed and is unique")
        void updateCard_Success_WhenNumberChangedAndUnique() {
            Long cardId = 10L;
            PaymentCardRequestDto requestDto = new PaymentCardRequestDto(
                    "8765432187654321", "New Holder", LocalDate.of(2035, 1, 1), true, 1L
            );

            PaymentCard existingCard = new PaymentCard();
            existingCard.setId(cardId);
            existingCard.setNumber("1234567812345678");

            PaymentCardResponseDto expectedResponse = new PaymentCardResponseDto(
                    cardId, "8765432187654321", "New Holder", LocalDate.of(2035, 1, 1), true, 1L, null, null
            );

            when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
            when(paymentCardRepository.existsByNumber(requestDto.number())).thenReturn(false);
            when(paymentCardMapper.toDto(existingCard)).thenReturn(expectedResponse);

            PaymentCardResponseDto result = paymentCardService.updateCard(cardId, requestDto);

            assertThat(result).isNotNull();
            assertThat(existingCard.getNumber()).isEqualTo("8765432187654321");
            verify(paymentCardRepository, times(1)).existsByNumber(requestDto.number());
        }

        @Test
        @DisplayName("Throw BusinessException when changing card number to one that is already in use")
        void updateCard_ThrowsBusinessException_WhenNumberAlreadyInUse() {
            Long cardId = 10L;
            PaymentCardRequestDto requestDto = new PaymentCardRequestDto(
                    "8765432187654321", "New Holder", LocalDate.of(2035, 1, 1), true, 1L
            );

            PaymentCard existingCard = new PaymentCard();
            existingCard.setId(cardId);
            existingCard.setNumber("1234567812345678");

            when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
            when(paymentCardRepository.existsByNumber(requestDto.number())).thenReturn(true);

            assertThatThrownBy(() -> paymentCardService.updateCard(cardId, requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Number 8765432187654321 is already in use by another card");
        }
    }

    @Nested
    @DisplayName("Updating card active status")
    class UpdateCardStatusTests {

        @Test
        @DisplayName("Update card active status success")
        void updateCardStatus_Success() {
            Long cardId = 10L;
            PaymentCard card = new PaymentCard();
            card.setId(cardId);
            card.setActive(true);

            PaymentCardResponseDto expectedResponse = new PaymentCardResponseDto(
                    cardId, "1234567812345678", "Egor", LocalDate.now(), false, 1L, null, null
            );

            when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));
            when(paymentCardMapper.toDto(card)).thenReturn(expectedResponse);

            PaymentCardResponseDto result = paymentCardService.updateCardStatus(cardId, false);

            assertThat(result).isNotNull();
            assertThat(card.getActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Deleting card")
    class DeleteCardTests {

        @Test
        @DisplayName("Delete card success and return owner userId")
        void deleteCard_Success() {
            Long cardId = 10L;
            Long userId = 1L;

            User user = new User();
            user.setId(userId);

            PaymentCard card = new PaymentCard();
            card.setId(cardId);
            card.setUser(user);

            when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));

            Long returnedUserId = paymentCardService.deleteCard(cardId);

            assertThat(returnedUserId).isEqualTo(userId);
            verify(paymentCardRepository, times(1)).deleteById(cardId);
        }
    }
}