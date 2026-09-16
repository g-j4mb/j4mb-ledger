package com.j4mb.ledger.coa.api;

import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.service.CoaService;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Chart of Accounts", description = "Manage the COA hierarchy. Leaf nodes (postable=true) can be linked to operational accounts.")
@RestController
@RequestMapping("/api/v1/coa")
class CoaController {

    private final CoaService coaService;

    CoaController(CoaService coaService) {
        this.coaService = coaService;
    }

    @Operation(summary = "List COA nodes", description = "Returns root nodes when parentId is omitted. Pass parentId to drill into a subtree.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "COA nodes returned")
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<CoaNodeResponse>>> list(
            @Parameter(description = "Filter by parent node UUID. Omit to list root nodes.", required = false)
            @RequestParam(required = false) UUID parentId) {
        List<CoaNode> nodes = parentId == null
                ? coaService.listRoots()
                : coaService.listChildren(parentId);
        return ResponseEntity.ok(ApiResponse.success(nodes.stream().map(CoaNodeResponse::from).toList()));
    }

    @Operation(summary = "Create COA node", description = "Adds a node to the chart of accounts. Root nodes have no parentId. Leaf nodes should be marked postable=true to allow account creation.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "COA node created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failure"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Code already exists or parent node not found")
    })
    @PostMapping
    ResponseEntity<ApiResponse<CoaNodeResponse>> create(@Valid @RequestBody CreateCoaNodeRequest req) {
        CoaNode node = coaService.create(req.parentId(), req.code(), req.name(),
                req.accountType(), req.normalBalance(), req.postable(), req.displayOrder(),
                req.allowNegativeBalance());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CoaNodeResponse.from(node)));
    }

    @Operation(summary = "Update COA node", description = "Updates mutable fields. Code and accountType are immutable.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "COA node not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<CoaNodeResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateCoaNodeRequest req) {
        CoaNode node = coaService.update(id, req.name(), req.description(), req.displayOrder(),
                req.allowNegativeBalance(), req.nodeRole(), req.postable());
        return ResponseEntity.ok(ApiResponse.success(CoaNodeResponse.from(node)));
    }

    @Operation(summary = "Delete COA node", description = "Deletes node only if it has no children and no linked accounts.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Has children or linked accounts")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        coaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
