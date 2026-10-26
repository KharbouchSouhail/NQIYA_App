package ma.nqiya.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LoginCodeRepository extends JpaRepository<LoginCode, Long> {

    Optional<LoginCode> findFirstByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, Instant after);

    @Modifying
    @Query("update LoginCode c set c.consumed = true where c.email = :email and c.consumed = false")
    void consumeAllFor(String email);

    @Modifying
    @Query("delete from LoginCode c where c.createdAt < :before")
    int deleteOlderThan(Instant before);
}
