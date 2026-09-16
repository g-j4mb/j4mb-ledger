package com.j4mb.ledger.journal.api;

import com.j4mb.ledger.journal.domain.JournalHead;
import com.j4mb.ledger.journal.domain.JournalLine;
import com.j4mb.ledger.journal.service.JournalService;
import com.j4mb.ledger.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Journals", description = "Core double-entry accounting engine. Create DRAFT journals with balanced debit/credit lines, then POST them to make them immutable.")
@RestController
@RequestMapping("/api/v1/journals")
class JournalController {

    private final JournalService journalService;

    JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @Operation(
        summary = "Create draft journal",
        description = "Creates a DRAFT journal entry with debit and credit lines. " +
            "The double-entry invariant (SUM(debits) = SUM(credits)) is validated on posting, not creation. " +
            "DRAFT journals can be inspected before committing.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Draft journal created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure — missing fields or invalid line amounts"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Fiscal period not found")
    })
    @PostMapping
    ResponseEntity<ApiResponse<JournalResponse>> create(@Valid @RequestBody CreateJournalRequest req) {
        JournalHead head = journalService.createDraft(
                req.fiscalPeriodId(), req.transactionType(), req.currencyCode(),
                req.description(), req.reference(), req.sourceSystem(), req.sourceDocumentId(),
                req.lines());
        List<JournalLine> lines = journalService.findLines(head.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(JournalResponse.from(head,
                        lines.stream().map(JournalLineResponse::from).toList())));
    }

    @Operation(summary = "Get journal", description = "Returns a journal entry with all its debit/credit lines.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Journal returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Journal not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<JournalResponse>> get(@PathVariable UUID id) {
        JournalHead head  = journalService.findById(id);
        List<JournalLine> lines = journalService.findLines(id);
        return ResponseEntity.ok(ApiResponse.success(JournalResponse.from(head,
                lines.stream().map(JournalLineResponse::from).toList())));
    }

    @Operation(
        summary = "Post journal",
        description = "Transitions a DRAFT journal to POSTED — the terminal immutable state. " +
            "Validates: fiscal period is not LOCKED, and SUM(debits) = SUM(credits). " +
            "Once POSTED, corrections require a new REVERSAL journal.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Journal posted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Journal not in DRAFT status, fiscal period locked, or journal unbalanced")
    })
    @PostMapping("/{id}/post")
    ResponseEntity<ApiResponse<JournalResponse>> post(
            @PathVariable UUID id,
            @Parameter(name = "X-Posted-By", description = "Identity posting the journal (user email or system name).", example = "admin@j4mb.com", in = ParameterIn.HEADER)
            @RequestHeader(value = "X-Posted-By", defaultValue = "system") String postedBy) {
        JournalHead posted = journalService.post(id, postedBy);
        List<JournalLine> lines = journalService.findLines(id);
        return ResponseEntity.ok(ApiResponse.success(JournalResponse.from(posted,
                lines.stream().map(JournalLineResponse::from).toList())));
    }

    @Operation(summary = "Update draft journal", description = "Updates description and reference of a DRAFT journal.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Journal not found or not in DRAFT status")
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<JournalResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateJournalRequest req) {
        JournalHead head = journalService.updateDraft(id, req.description(), req.reference());
        List<JournalLine> lines = journalService.findLines(id);
        return ResponseEntity.ok(ApiResponse.success(JournalResponse.from(head,
                lines.stream().map(JournalLineResponse::from).toList())));
    }

    @Operation(summary = "Add line to draft journal", description = "Appends a debit or credit line to a DRAFT journal.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Line added"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Journal not found or not in DRAFT status")
    })
    @PostMapping("/{id}/lines")
    ResponseEntity<ApiResponse<JournalLineResponse>> addLine(@PathVariable UUID id,
            @Valid @RequestBody JournalLineRequest req) {
        JournalLine line = journalService.addLine(id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(JournalLineResponse.from(line)));
    }

    @Operation(summary = "Remove line from draft journal", description = "Removes a specific line from a DRAFT journal.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Line removed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Journal or line not found, or journal not DRAFT")
    })
    @DeleteMapping("/{id}/lines/{lineId}")
    ResponseEntity<Void> removeLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        journalService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel draft journal",
        description = "Cancels a DRAFT journal. POSTED journals cannot be cancelled — use a REVERSAL journal instead.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Journal cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Journal is not in DRAFT status")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> cancel(@PathVariable UUID id) {
        journalService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
