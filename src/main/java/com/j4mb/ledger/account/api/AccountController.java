package com.j4mb.ledger.account.api;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.service.AccountService;
import com.j4mb.ledger.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Accounts", description = "Operational accounts linked to COA leaf nodes. Each account is denominated in a single currency.")
@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

    private final AccountService accountService;

    AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "List active accounts", description = "Returns a paginated list of ACTIVE accounts for the current tenant.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account page returned")
    })
    @GetMapping
    ResponseEntity<ApiResponse<Page<AccountResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AccountResponse> page = accountService.listActive(pageable).map(AccountResponse::from);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Operation(summary = "Get account", description = "Returns a single account by UUID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Account not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<AccountResponse>> get(@PathVariable UUID id) {
        Account account = accountService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @Operation(summary = "Create account", description = "Creates an operational account linked to a postable COA leaf node. Account numbers must be unique within the tenant.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "COA node not found or not postable, or account number already exists")
    })
    @PostMapping
    ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody CreateAccountRequest req) {
        Account account = accountService.create(
                req.coaNodeId(), req.accountNumber(), req.name(), req.currencyCode());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AccountResponse.from(account)));
    }

    @Operation(summary = "Update account", description = "Updates mutable fields. Account number and currency are immutable.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Account not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<AccountResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest req) {
        Account account = accountService.update(id, req.name(), req.description(),
                req.overdraftLimit(), req.externalRef());
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account)));
    }

    @Operation(summary = "Delete account", description = "Deletes account only if it has no journal line history.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Account has journal history")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
