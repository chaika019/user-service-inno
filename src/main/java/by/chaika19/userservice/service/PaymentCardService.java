package by.chaika19.userservice.service;

import by.chaika19.userservice.dto.PaymentCardRequestDto;
import by.chaika19.userservice.dto.PaymentCardResponseDto;
import by.chaika19.userservice.exception.BusinessException;
import by.chaika19.userservice.exception.ResourceNotFoundException;
import by.chaika19.userservice.mapper.PaymentCardMapper;
import by.chaika19.userservice.model.PaymentCard;
import by.chaika19.userservice.model.User;
import by.chaika19.userservice.repository.PaymentCardRepository;
import by.chaika19.userservice.repository.PaymentCardSpecification;
import by.chaika19.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCardService {

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardResponseDto create(PaymentCardRequestDto paymentCardRequestDto) {
        User user = userRepository.findById(paymentCardRequestDto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + paymentCardRequestDto.userId()));

        if(user.getCards().size() >= 5) {
            throw new BusinessException("User already has maximum number of cards 5");
        }

        PaymentCard paymentCard = paymentCardMapper.toEntity(paymentCardRequestDto);
        paymentCard.setUser(user);

        return paymentCardMapper.toDto(paymentCardRepository.save(paymentCard));
    }

    @Cacheable(value = "cards", key = "#id")
    public PaymentCardResponseDto findById(Long id) {
        return paymentCardRepository.findById(id)
                .map(paymentCardMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
    }

    public Page<PaymentCardResponseDto> findAllCards(String name, String surname, Pageable pageable) {
        Specification<PaymentCard> specification = Specification.where(PaymentCardSpecification.hasName(name))
                .and(PaymentCardSpecification.hasSurname(surname));

        return paymentCardRepository.findAll(specification, pageable)
                .map(paymentCardMapper::toDto);
    }

    public List<PaymentCardResponseDto> findAllByUserId(Long userId) {
        return paymentCardRepository.findAllByUserId(userId)
                .stream()
                .map(paymentCardMapper::toDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardResponseDto updateCard(Long id, PaymentCardRequestDto paymentCardRequestDto) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));

        if (!card.getNumber().equals(paymentCardRequestDto.number()) &&
                paymentCardRepository.existsByNumber(paymentCardRequestDto.number())) {
            throw new BusinessException("Number " + paymentCardRequestDto.number() + " is already in use by another card");
        }

        card.setNumber(paymentCardRequestDto.number());
        card.setHolder(paymentCardRequestDto.holder());
        card.setExpirationDate(paymentCardRequestDto.expirationDate());

        return paymentCardMapper.toDto(card);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardResponseDto updateCardStatus(Long id, Boolean active) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment card not found with id: " + id));

        card.setActive(active);
        return paymentCardMapper.toDto(card);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result")
    public Long deleteCard(Long id) {
        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card with id " + id + " not found"));

        Long userId = paymentCard.getUser().getId();
        paymentCardRepository.deleteById(id);
        return userId;
    }
}
