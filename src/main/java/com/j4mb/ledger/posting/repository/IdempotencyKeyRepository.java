package com.j4mb.ledger.posting.repository;
import com.j4mb.ledger.posting.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findBySourceSystemAndSourceEventId(String sourceSystem, String sourceEventId);
}
