package com.j4mb.ledger.closing.api;

import com.j4mb.ledger.closing.service.PeriodClosingService;
import com.j4mb.ledger.closing.service.YearEndClosingService;
import com.j4mb.ledger.fiscal.api.FiscalPeriodResponse;
import com.j4mb.ledger.fiscal.api.FiscalYearResponse;
import com.j4mb.ledger.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Period & Year Closing", description = "Control the lifecycle of fiscal periods and years.")
@RestController
@RequestMapping("/api/v1/fiscal")
class ClosingController {

    private final PeriodClosingService  periodClosingService;
    private final YearEndClosingService yearEndClosingService;

    ClosingController(PeriodClosingService periodClosingService,
                      YearEndClosingService yearEndClosingService) {
        this.periodClosingService  = periodClosingService;
        this.yearEndClosingService = yearEndClosingService;
    }

    @Operation(summary = "Close fiscal period",
        description = "Marks period CLOSED. All DRAFT journals must be posted or cancelled first.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Period closed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Period not OPEN or has outstanding DRAFTs")
    })
    @PostMapping("/periods/{periodId}/close")
    ResponseEntity<ApiResponse<FiscalPeriodResponse>> closePeriod(
            @PathVariable UUID periodId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                FiscalPeriodResponse.from(periodClosingService.closePeriod(periodId, userId))));
    }

    @Operation(summary = "Reopen fiscal period",
        description = "Reopens a CLOSED period to allow adjusting entries. LOCKED periods cannot be reopened.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Period reopened"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Period is not CLOSED (may be LOCKED)")
    })
    @PostMapping("/periods/{periodId}/reopen")
    ResponseEntity<ApiResponse<FiscalPeriodResponse>> reopenPeriod(
            @PathVariable UUID periodId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                FiscalPeriodResponse.from(periodClosingService.reopenPeriod(periodId, userId))));
    }

    @Operation(summary = "Lock fiscal period",
        description = "Permanently locks a CLOSED period. No posting or reopening is possible after locking.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Period locked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Period is not CLOSED")
    })
    @PostMapping("/periods/{periodId}/lock")
    ResponseEntity<ApiResponse<FiscalPeriodResponse>> lockPeriod(
            @PathVariable UUID periodId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                FiscalPeriodResponse.from(periodClosingService.lockPeriod(periodId, userId))));
    }

    @Operation(summary = "Close fiscal year (year-end)",
        description = "Runs year-end closing: zeros revenue/expense accounts via closing journals, " +
            "transfers net to Retained Earnings, marks year CLOSED. All periods must be CLOSED or LOCKED first.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Year closed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Year not OPEN or has open periods")
    })
    @PostMapping("/years/{yearId}/close")
    ResponseEntity<ApiResponse<FiscalYearResponse>> closeYear(@PathVariable UUID yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                FiscalYearResponse.from(yearEndClosingService.closeYear(yearId))));
    }
}
