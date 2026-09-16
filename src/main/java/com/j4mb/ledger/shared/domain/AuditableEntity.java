package com.j4mb.ledger.shared.domain;

import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc = Instant.now();

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAtUtc = Instant.now();
    }

    public Instant getCreatedAtUtc() { return createdAtUtc; }
    public Instant getUpdatedAtUtc() { return updatedAtUtc; }
    public String  getCreatedBy()    { return createdBy; }
    public String  getUpdatedBy()    { return updatedBy; }
    public Long    getVersion()      { return version; }

    protected void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    protected void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
