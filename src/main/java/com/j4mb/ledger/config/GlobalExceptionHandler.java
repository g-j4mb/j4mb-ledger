package com.j4mb.ledger.config;

import com.j4mb.ledger.provisioning.TenantAlreadyExistsException;
import com.j4mb.ledger.provisioning.TenantProvisioningException;
import com.j4mb.ledger.shared.api.ApiResponse;
import com.j4mb.ledger.shared.exception.ClosedPeriodException;
import com.j4mb.ledger.shared.exception.InsufficientBalanceException;
import com.j4mb.ledger.shared.exception.LedgerException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantAlreadyExists(TenantAlreadyExistsException ex) {
        log.warn("Tenant provisioning conflict: traceId={}, message={}", MDC.get("traceId"), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("TENANT_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(TenantProvisioningException.class)
    public ResponseEntity<ApiResponse<Void>> handleProvisioningFailure(TenantProvisioningException ex) {
        log.error("Tenant provisioning failed: traceId={}, tenant={}", MDC.get("traceId"), ex.getTenantCode(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PROVISIONING_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(ClosedPeriodException.class)
    public ResponseEntity<ApiResponse<Void>> handleClosedPeriod(ClosedPeriodException ex) {
        log.warn("Posting rejected — period closed: traceId={}, message={}", MDC.get("traceId"), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("PERIOD_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Overdraft rejected: traceId={}, message={}", MDC.get("traceId"), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("INSUFFICIENT_BALANCE", ex.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Concurrent balance update conflict: traceId={}", MDC.get("traceId"));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("BALANCE_CONCURRENCY_CONFLICT",
                        "Concurrent balance update detected — please retry the posting"));
    }

    @ExceptionHandler(LedgerException.class)
    public ResponseEntity<ApiResponse<Void>> handleLedgerException(LedgerException ex, HttpServletRequest request) {
        log.warn("Ledger business error: traceId={}, path={}, message={}", MDC.get("traceId"), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("LEDGER_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError(details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: traceId={}, path={}", MDC.get("traceId"), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
