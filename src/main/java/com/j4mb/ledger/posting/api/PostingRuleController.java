package com.j4mb.ledger.posting.api;

import com.j4mb.ledger.posting.domain.PostingRule;
import com.j4mb.ledger.posting.service.PostingRuleService;
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

@Tag(name = "Posting Rules", description = "Configure business-event-to-journal-entry mapping rules for the automated posting engine.")
@RestController
@RequestMapping("/api/v1/posting-rules")
class PostingRuleController {

    private final PostingRuleService postingRuleService;

    PostingRuleController(PostingRuleService postingRuleService) {
        this.postingRuleService = postingRuleService;
    }

    @Operation(summary = "List active posting rules", description = "Returns a paginated list of active posting rules for the current tenant.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posting rules returned")
    })
    @GetMapping
    ResponseEntity<ApiResponse<Page<PostingRuleResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostingRuleResponse> page = postingRuleService.listActive(pageable)
                .map(PostingRuleResponse::from);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Operation(summary = "Get posting rule", description = "Returns a single posting rule by UUID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posting rule returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Posting rule not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<PostingRuleResponse>> get(@PathVariable UUID id) {
        PostingRule rule = postingRuleService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(PostingRuleResponse.from(rule)));
    }

    @Operation(summary = "Create posting rule", description = "Defines a new business-event-to-journal-entry mapping rule. The rule is active immediately.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Posting rule created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure")
    })
    @PostMapping
    ResponseEntity<ApiResponse<PostingRuleResponse>> create(
            @Valid @RequestBody CreatePostingRuleRequest req) {
        PostingRule rule = postingRuleService.create(
                req.eventType(), req.ruleName(), req.transactionType(),
                req.debitCoaCode(), req.creditCoaCode(), req.descriptionTemplate(),
                (short) req.priority(), req.conditions(), req.multiLegRules());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PostingRuleResponse.from(rule)));
    }

    @Operation(summary = "Update posting rule", description = "Updates mutable fields. eventType and transactionType are immutable.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Posting rule not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<PostingRuleResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdatePostingRuleRequest req) {
        PostingRule rule = postingRuleService.update(id, req.debitCoaCode(), req.creditCoaCode(),
                req.descriptionTemplate(), req.active(), req.priority(),
                req.conditions(), req.multiLegRules());
        return ResponseEntity.ok(ApiResponse.success(PostingRuleResponse.from(rule)));
    }

    @Operation(summary = "Deactivate posting rule", description = "Soft-deletes a posting rule by setting active=false. The rule is no longer used by the posting engine but is retained for audit.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Posting rule not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<PostingRuleResponse>> deactivate(@PathVariable UUID id) {
        PostingRule rule = postingRuleService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(PostingRuleResponse.from(rule)));
    }
}
