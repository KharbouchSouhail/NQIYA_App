package ma.nqiya.spot;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    Optional<Participation> findBySpotIdAndUserId(Long spotId, Long userId);

    boolean existsBySpotIdAndUserId(Long spotId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<Participation> findBySpotIdOrderByJoinedAtAsc(Long spotId);

    @EntityGraph(attributePaths = "user")
    @Query("select p from Participation p where p.spot.id = :spotId and p.checkedInAt is not null")
    List<Participation> findCheckedIn(Long spotId);

    long countBySpotIdAndCheckedInAtIsNotNull(Long spotId);

    @EntityGraph(attributePaths = {"spot", "spot.organizer"})
    @Query("select p from Participation p where p.user.id = :userId order by p.joinedAt desc")
    List<Participation> findByUser(Long userId);

    @Query("select count(p) from Participation p where p.user.id = :userId and p.checkedInAt is not null and p.spot.status = ma.nqiya.spot.SpotStatus.CLEAN")
    long countCompletedCleanups(Long userId);
}
