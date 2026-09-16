package com.j4mb.ledger.audit.repository;
import com.j4mb.ledger.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtUtcDesc(String entityType, UUID entityId);
}
