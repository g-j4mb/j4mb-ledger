package com.j4mb.ledger.provisioning;

/**
 * Thrown when attempting to provision a tenant whose {@code tenantCode} is already registered.
 */
public class TenantAlreadyExistsException extends RuntimeException {

    private final String tenantCode;

    public TenantAlreadyExistsException(String tenantCode) {
        super("Tenant already exists: " + tenantCode);
        this.tenantCode = tenantCode;
    }

    public String getTenantCode() {
        return tenantCode;
    }
}
