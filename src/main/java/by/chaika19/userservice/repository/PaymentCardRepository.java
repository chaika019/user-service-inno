package by.chaika19.userservice.repository;

import by.chaika19.userservice.model.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

    @Query("SELECT c FROM PaymentCard c WHERE c.user.id = :userId")
    List<PaymentCard> findAllByUserId(@Param("userId") Long userId);

    boolean existsByNumber(String number);
}
