package com.j4mb.ledger.currency.api;

import com.j4mb.ledger.currency.domain.ExchangeRate;
import com.j4mb.ledger.currency.service.ExchangeRateService;
import com.j4mb.ledger.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Exchange Rates", description = "Manage FX exchange rates for the tenant.")
@RestController
@RequestMapping("/api/v1/exchange-rates")
class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Operation(summary = "List exchange rates", description = "Returns all FX exchange rates for the current tenant.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Exchange rates returned")
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                exchangeRateService.listAll().stream().map(ExchangeRateResponse::from).toList()));
    }

    @Operation(summary = "Update exchange rate", description = "Updates the rate value and/or source.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Exchange rate not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<ExchangeRateResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateExchangeRateRequest req) {
        ExchangeRate er = exchangeRateService.update(id, req.rate(), req.source());
        return ResponseEntity.ok(ApiResponse.success(ExchangeRateResponse.from(er)));
    }

    @Operation(summary = "Delete exchange rate", description = "Permanently removes an exchange rate record.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Exchange rate not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        exchangeRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
