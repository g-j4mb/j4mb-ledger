package com.j4mb.ledger.provisioning;

/**
 * Thrown when tenant provisioning fails after schema creation has begun.
 * The schema may or may not have been cleaned up — check logs for details.
 */
public class TenantProvisioningException extends RuntimeException {

    private final String tenantCode;

    public TenantProvisioningException(String tenantCode, Throwable cause) {
        super("Provisioning failed for tenant '" + tenantCode + "': " + cause.getMessage(), cause);
        this.tenantCode = tenantCode;
    }

    public String getTenantCode() {
        return tenantCode;
    }
}
