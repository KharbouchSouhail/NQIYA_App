package ma.nqiya.reward;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardEventRepository extends JpaRepository<RewardEvent, Long> {

    List<RewardEvent> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}
