package by.chaika19.userservice.repository;

import by.chaika19.userservice.model.PaymentCard;
import by.chaika19.userservice.model.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class PaymentCardSpecification {

    public static Specification<PaymentCard> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return null;
            }
            Join<PaymentCard, User> userJoin = root.join("user");
            return criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("name")), name.toLowerCase() + "%");
        };
    }

    public static Specification<PaymentCard> hasSurname(String surname) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(surname)) {
                return null;
            }
            Join<PaymentCard, User> userJoin = root.join("user");
            return criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("surname")), surname.toLowerCase() + "%");
        };
    }
}
