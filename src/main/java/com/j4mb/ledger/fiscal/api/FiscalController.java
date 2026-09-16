package com.j4mb.ledger.fiscal.api;

import com.j4mb.ledger.fiscal.domain.FiscalPeriod;
import com.j4mb.ledger.fiscal.domain.FiscalYear;
import com.j4mb.ledger.fiscal.service.FiscalPeriodService;
import com.j4mb.ledger.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Fiscal", description = "Manage fiscal years and periods. Periods must be OPEN to accept journal postings.")
@RestController
@RequestMapping("/api/v1/fiscal")
class FiscalController {

    private final FiscalPeriodService fiscalPeriodService;

    FiscalController(FiscalPeriodService fiscalPeriodService) {
        this.fiscalPeriodService = fiscalPeriodService;
    }

    @Operation(summary = "List fiscal years", description = "Returns all fiscal years for the current tenant, ordered by start date descending.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fiscal years returned")
    })
    @GetMapping("/years")
    ResponseEntity<ApiResponse<List<FiscalYearResponse>>> listYears() {
        List<FiscalYearResponse> result = fiscalPeriodService.listYears().stream()
                .map(FiscalYearResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Create fiscal year", description = "Creates a new fiscal year in OPEN status. Year names must be unique.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Fiscal year created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Year name already exists or date range invalid")
    })
    @PostMapping("/years")
    ResponseEntity<ApiResponse<FiscalYearResponse>> createYear(@Valid @RequestBody CreateFiscalYearRequest req) {
        FiscalYear year = fiscalPeriodService.createYear(req.yearName(), req.startDate(), req.endDate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(FiscalYearResponse.from(year)));
    }

    @Operation(summary = "List fiscal periods", description = "Returns all periods within a fiscal year.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Periods returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Fiscal year not found")
    })
    @GetMapping("/years/{yearId}/periods")
    ResponseEntity<ApiResponse<List<FiscalPeriodResponse>>> listPeriods(@PathVariable UUID yearId) {
        List<FiscalPeriodResponse> result = fiscalPeriodService.listPeriods(yearId).stream()
                .map(FiscalPeriodResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Create fiscal period", description = "Adds a period (e.g. monthly) to a fiscal year. Periods are created in OPEN status and can accept journal postings.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Period created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Fiscal year not found")
    })
    @PostMapping("/years/{yearId}/periods")
    ResponseEntity<ApiResponse<FiscalPeriodResponse>> createPeriod(
            @PathVariable UUID yearId,
            @Valid @RequestBody CreateFiscalPeriodRequest req) {
        FiscalPeriod period = fiscalPeriodService.createPeriod(
                yearId, req.periodName(), req.periodNumber(), req.startDate(), req.endDate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(FiscalPeriodResponse.from(period)));
    }

    @Operation(summary = "Find current fiscal period", description = "Returns the fiscal period that contains the given date (defaults to today). Used to resolve the active period for journal posting.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current period returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "No fiscal period found for the given date")
    })
    @GetMapping("/periods/current")
    ResponseEntity<ApiResponse<FiscalPeriodResponse>> currentPeriod(
            @Parameter(description = "Date in ISO format (yyyy-MM-dd). Defaults to today.", example = "2026-01-15")
            @RequestParam(defaultValue = "") String date) {
        LocalDate target = date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        FiscalPeriod period = fiscalPeriodService.findForDate(target);
        return ResponseEntity.ok(ApiResponse.success(FiscalPeriodResponse.from(period)));
    }

    @Operation(summary = "Update fiscal year", description = "Updates yearName, startDate, or endDate of an OPEN fiscal year with no posted journals.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Year not found, not OPEN, or has posted journals")
    })
    @PutMapping("/years/{yearId}")
    ResponseEntity<ApiResponse<FiscalYearResponse>> updateYear(
            @PathVariable UUID yearId,
            @Valid @RequestBody UpdateFiscalYearRequest req) {
        FiscalYear year = fiscalPeriodService.updateYear(yearId, req.yearName(), req.startDate(), req.endDate());
        return ResponseEntity.ok(ApiResponse.success(FiscalYearResponse.from(year)));
    }

    @Operation(summary = "Delete fiscal year", description = "Deletes an OPEN fiscal year that has no periods.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Year not found, not OPEN, or has periods")
    })
    @DeleteMapping("/years/{yearId}")
    ResponseEntity<Void> deleteYear(@PathVariable UUID yearId) {
        fiscalPeriodService.deleteYear(yearId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update fiscal period", description = "Updates periodName, startDate, or endDate of an OPEN period with no posted journals.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Period not found, not OPEN, or has posted journals")
    })
    @PutMapping("/years/{yearId}/periods/{periodId}")
    ResponseEntity<ApiResponse<FiscalPeriodResponse>> updatePeriod(
            @PathVariable UUID yearId,
            @PathVariable UUID periodId,
            @Valid @RequestBody UpdateFiscalPeriodRequest req) {
        FiscalPeriod period = fiscalPeriodService.updatePeriod(periodId, req.periodName(),
                req.startDate(), req.endDate());
        return ResponseEntity.ok(ApiResponse.success(FiscalPeriodResponse.from(period)));
    }

    @Operation(summary = "Delete fiscal period", description = "Deletes an OPEN period with no journals.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Period not found, not OPEN, or has journals")
    })
    @DeleteMapping("/years/{yearId}/periods/{periodId}")
    ResponseEntity<Void> deletePeriod(@PathVariable UUID yearId, @PathVariable UUID periodId) {
        fiscalPeriodService.deletePeriod(periodId);
        return ResponseEntity.noContent().build();
    }
}
