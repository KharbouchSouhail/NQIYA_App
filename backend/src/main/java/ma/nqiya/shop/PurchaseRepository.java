package ma.nqiya.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @EntityGraph(attributePaths = "voucher")
    List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    @EntityGraph(attributePaths = "voucher")
    Optional<Purchase> findByIdAndUserId(Long id, Long userId);

    boolean existsByCode(String code);
}
