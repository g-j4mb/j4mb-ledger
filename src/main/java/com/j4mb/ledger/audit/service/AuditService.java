package com.j4mb.ledger.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j4mb.ledger.audit.domain.AuditLog;
import com.j4mb.ledger.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper       objectMapper;

    AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper       = objectMapper;
    }

    public void record(String entityType, UUID entityId, String action,
                       String performedBy, Object before, Object after) {
        try {
            String beforeJson = before != null ? objectMapper.writeValueAsString(before) : null;
            String afterJson  = after  != null ? objectMapper.writeValueAsString(after)  : null;
            auditLogRepository.save(AuditLog.create(entityType, entityId, action, performedBy, beforeJson, afterJson));
        } catch (Exception e) {
            log.error("Failed to write audit log: entity={}/{}, action={}", entityType, entityId, action, e);
        }
    }
}
