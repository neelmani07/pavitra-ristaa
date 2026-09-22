package com.pavitraristaa.connections.controller;

import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.connections.dto.CreateInterestRequest;
import com.pavitraristaa.connections.dto.InterestResponse;
import com.pavitraristaa.connections.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interests")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Connections")
public class InterestController {

    private final InterestService interestService;
    private final CurrentUserAccessor currentUserAccessor;

    public InterestController(InterestService interestService, CurrentUserAccessor currentUserAccessor) {
        this.interestService = interestService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send an interest")
    public ApiResponse<InterestResponse> send(@Valid @RequestBody CreateInterestRequest request) {
        return ApiResponse.ok(interestService.send(currentUserAccessor.requireUser(), request), "Interest sent");
    }

    @GetMapping("/sent")
    @Operation(summary = "List sent interests")
    public ApiResponse<List<InterestResponse>> sent(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(interestService.listSent(currentUserAccessor.requireUser(), page, size), "Sent interests");
    }

    @GetMapping("/received")
    @Operation(summary = "List received interests")
    public ApiResponse<List<InterestResponse>> received(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(interestService.listReceived(currentUserAccessor.requireUser(), page, size), "Received interests");
    }

    @GetMapping("/{interestId}")
    @Operation(summary = "Get interest details")
    public ApiResponse<InterestResponse> getOne(@PathVariable UUID interestId) {
        return ApiResponse.ok(interestService.getOne(currentUserAccessor.requireUser(), interestId), "Interest");
    }

    @PostMapping("/{interestId}/accept")
    @Operation(summary = "Accept received interest")
    public ApiResponse<InterestResponse> accept(@PathVariable UUID interestId) {
        return ApiResponse.ok(interestService.accept(currentUserAccessor.requireUser(), interestId), "Interest accepted");
    }

    @PostMapping("/{interestId}/decline")
    @Operation(summary = "Decline received interest")
    public ApiResponse<InterestResponse> decline(@PathVariable UUID interestId) {
        return ApiResponse.ok(interestService.decline(currentUserAccessor.requireUser(), interestId), "Interest declined");
    }

    @PostMapping("/{interestId}/withdraw")
    @Operation(summary = "Withdraw sent interest")
    public ApiResponse<InterestResponse> withdraw(@PathVariable UUID interestId) {
        return ApiResponse.ok(interestService.withdraw(currentUserAccessor.requireUser(), interestId), "Interest withdrawn");
    }
}
