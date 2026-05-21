package by.chaika19.userservice.repository;

import by.chaika19.userservice.model.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecification {

    public static Specification<User> hasName(String name) {
        return ((root, query, criteriaBuilder) -> {
            if(!StringUtils.hasText(name)) {
                return null;
            } else {
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), name.toLowerCase() + "%");
            }
        });
    }

    public static Specification<User> hasSurname(String surname) {
        return ((root, query, criteriaBuilder) -> {
            if(!StringUtils.hasText(surname)) {
                return null;
            } else {
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("surname")), surname.toLowerCase() + "%");
            }
        });
    }
}
