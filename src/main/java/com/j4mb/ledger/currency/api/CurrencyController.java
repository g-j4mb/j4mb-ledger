package com.j4mb.ledger.currency.api;

import com.j4mb.ledger.currency.domain.Currency;
import com.j4mb.ledger.currency.service.CurrencyService;
import com.j4mb.ledger.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import java.util.List;

@Tag(name = "Currencies", description = "Manage tenant currencies and designate the base currency.")
@RestController
@RequestMapping("/api/v1/currencies")
class CurrencyController {

    private final CurrencyService currencyService;

    CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Operation(summary = "List currencies", description = "Returns all currencies configured for the current tenant.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Currency list returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid X-Tenant-Code header")
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<CurrencyResponse>>> list() {
        List<CurrencyResponse> result = currencyService.listAll().stream()
                .map(CurrencyResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Create currency", description = "Adds a currency to the tenant. Only one currency may be designated as the base currency.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Currency created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure or missing X-Tenant-Code"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Currency code already exists or base currency already configured")
    })
    @PostMapping
    ResponseEntity<ApiResponse<CurrencyResponse>> create(@Valid @RequestBody CreateCurrencyRequest request) {
        Currency currency = currencyService.create(
                request.currencyCode(), request.currencyName(),
                request.baseCurrency(), request.decimalPlaces());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CurrencyResponse.from(currency)));
    }

    @Operation(summary = "Update currency", description = "Updates name, active status, or decimal places. Currency code and base currency flag are immutable. Use deactivation (active=false) instead of deletion.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Currency not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<CurrencyResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateCurrencyRequest request) {
        Currency currency = currencyService.update(id, request.currencyName(),
                request.active(), request.decimalPlaces());
        return ResponseEntity.ok(ApiResponse.success(CurrencyResponse.from(currency)));
    }
}
