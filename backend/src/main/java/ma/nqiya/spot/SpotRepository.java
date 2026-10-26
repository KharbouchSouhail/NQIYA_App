package ma.nqiya.spot;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpotRepository extends JpaRepository<Spot, Long> {

    /** Open spots inside a bounding box; the exact radius is refined in memory with the haversine distance. */
    @EntityGraph(attributePaths = "organizer")
    @Query("""
            select s from Spot s
            where s.closed = false
              and s.status in :statuses
              and s.latitude between :minLat and :maxLat
              and s.longitude between :minLng and :maxLng
            """)
    List<Spot> findInBox(Collection<SpotStatus> statuses, double minLat, double maxLat, double minLng, double maxLng);

    @EntityGraph(attributePaths = "organizer")
    @Query("select s from Spot s where s.closed = false and s.status in :statuses order by s.createdAt desc")
    List<Spot> findOpenByStatus(Collection<SpotStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "organizer")
    Page<Spot> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(SpotStatus status);
}
