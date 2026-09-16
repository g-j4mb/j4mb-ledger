package com.j4mb.ledger.journal.repository;
import com.j4mb.ledger.journal.domain.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {
    List<JournalLine> findByJournalIdOrderByLineNumber(UUID journalId);
    List<JournalLine> findByAccountId(UUID accountId);
    boolean existsByAccountId(UUID accountId);
    Optional<JournalLine> findByJournalIdAndId(UUID journalId, UUID lineId);
}
