package com.j4mb.ledger.platform;

/**
 * Lifecycle status of a provisioned tenant.
 *
 * <p>State transitions:
 * <pre>
 *   PROVISIONING → ACTIVE → SUSPENDED → DEPROVISIONED
 *                         ↗
 *              (resume from SUSPENDED)
 * </pre>
 *
 * <p>{@code PROVISIONING} is a transient state used while schema creation and
 * migration are running. Once complete the status advances to {@code ACTIVE}.
 * A failed provisioning attempt leaves the row in {@code PROVISIONING} — the
 * cleanup process (or a retry) is responsible for resolving it.
 */
public enum TenantStatus {

    /** Schema creation and migration in progress. */
    PROVISIONING,

    /** Tenant is fully provisioned and accepting requests. */
    ACTIVE,

    /** Tenant access is temporarily blocked (e.g. overdue payment). */
    SUSPENDED,

    /** Tenant has been offboarded; schema may be archived or dropped. */
    DEPROVISIONED
}
