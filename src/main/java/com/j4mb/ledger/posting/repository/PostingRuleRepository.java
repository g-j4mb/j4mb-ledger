package com.j4mb.ledger.posting.repository;
import com.j4mb.ledger.posting.domain.PostingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface PostingRuleRepository extends JpaRepository<PostingRule, UUID> {
    List<PostingRule> findByEventTypeAndActiveTrueOrderByPriorityDesc(String eventType);
    Page<PostingRule> findByActiveTrue(Pageable pageable);
}
