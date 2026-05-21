package by.chaika19.userservice.controller;

import by.chaika19.userservice.dto.PaymentCardRequestDto;
import by.chaika19.userservice.dto.PaymentCardResponseDto;
import by.chaika19.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    public ResponseEntity<PaymentCardResponseDto> create(@Valid @RequestBody PaymentCardRequestDto paymentCardRequestDto) {
        PaymentCardResponseDto created = paymentCardService.create(paymentCardRequestDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> findById(@PathVariable Long id) {
        PaymentCardResponseDto paymentCardDto = paymentCardService.findById(id);
        return new ResponseEntity<>(paymentCardDto, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardResponseDto>> findAllCards(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<PaymentCardResponseDto> cards = paymentCardService.findAllCards(name, surname, pageable);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @GetMapping("/user/{user-id}")
    public ResponseEntity<List<PaymentCardResponseDto>> findAllByUserId(@PathVariable(name = "user-id") Long id) {
        List<PaymentCardResponseDto> cards = paymentCardService.findAllByUserId(id);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody PaymentCardRequestDto paymentCardRequestDto) {

        PaymentCardResponseDto updated = paymentCardService.updateCard(id, paymentCardRequestDto);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @PatchMapping("/{id}/status/{active}")
    public ResponseEntity<Void> updateCardStatus(
            @PathVariable Long id,
            @Valid @PathVariable Boolean active) {

        paymentCardService.updateCardStatus(id, active);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        paymentCardService.deleteCard(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
