package ma.nqiya.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByTicketCode(String ticketCode);

    boolean existsByTicketCode(String ticketCode);

    @Query("select u from User u order by u.xp desc, u.id asc")
    Page<User> findLeaderboard(Pageable pageable);

    @Query("select count(u) from User u where u.xp > :xp")
    long countWithMoreXp(int xp);

    Page<User> findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String email, String displayName, Pageable pageable);
}
