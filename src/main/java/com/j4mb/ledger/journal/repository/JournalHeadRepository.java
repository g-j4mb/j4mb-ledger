package com.j4mb.ledger.journal.repository;
import com.j4mb.ledger.journal.domain.JournalHead;
import com.j4mb.ledger.journal.domain.JournalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface JournalHeadRepository extends JpaRepository<JournalHead, UUID> {
    Optional<JournalHead> findByJournalNumber(String journalNumber);
    Page<JournalHead> findByFiscalPeriodId(UUID fiscalPeriodId, Pageable pageable);
    List<JournalHead> findBySourceSystemAndSourceDocumentId(String sourceSystem, String sourceDocumentId);
    boolean existsByFiscalPeriodIdAndStatus(UUID fiscalPeriodId, JournalStatus status);
    List<JournalHead> findByFiscalPeriodIdAndStatus(UUID fiscalPeriodId, JournalStatus status);
    boolean existsByFiscalPeriodId(UUID fiscalPeriodId);
    boolean existsByFiscalPeriodIdIn(List<UUID> periodIds);
}
