package com.pavitraristaa.auth.controller;

import com.pavitraristaa.auth.dto.AccountResponse;
import com.pavitraristaa.auth.dto.AppleLoginRequest;
import com.pavitraristaa.auth.dto.ChangePasswordRequest;
import com.pavitraristaa.auth.dto.DeleteAccountRequest;
import com.pavitraristaa.auth.dto.ForgotPasswordRequest;
import com.pavitraristaa.auth.dto.LinkAccountRequest;
import com.pavitraristaa.auth.dto.LinkedAccountsResponse;
import com.pavitraristaa.auth.dto.LoginOtpRequest;
import com.pavitraristaa.auth.dto.LoginRequest;
import com.pavitraristaa.auth.dto.OAuthLoginRequest;
import com.pavitraristaa.auth.dto.ReasonRequest;
import com.pavitraristaa.auth.dto.RefreshTokenRequest;
import com.pavitraristaa.auth.dto.RegisterRequest;
import com.pavitraristaa.auth.dto.RegisterResponse;
import com.pavitraristaa.auth.dto.ResendOtpRequest;
import com.pavitraristaa.auth.dto.ResetPasswordRequest;
import com.pavitraristaa.auth.dto.SessionResponse;
import com.pavitraristaa.auth.dto.TokenResponse;
import com.pavitraristaa.auth.dto.VerifyEmailRequest;
import com.pavitraristaa.auth.dto.VerifyOtpRequest;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.auth.service.DevicePlatformParser;
import com.pavitraristaa.auth.service.OAuthLoginService;
import com.pavitraristaa.common.api.ApiResponse;
import com.pavitraristaa.common.api.PagedData;
import com.pavitraristaa.common.security.CurrentUserAccessor;
import com.pavitraristaa.common.util.ClientContext;
import com.pavitraristaa.common.util.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final OAuthLoginService oAuthLoginService;
    private final CurrentUserAccessor currentUserAccessor;

    public AuthController(
            AuthService authService,
            OAuthLoginService oAuthLoginService,
            CurrentUserAccessor currentUserAccessor
    ) {
        this.authService = authService;
        this.oAuthLoginService = oAuthLoginService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new account")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Account registered");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify an OTP")
    public ApiResponse<AccountResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok(authService.verifyOtp(request), "OTP verified");
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend an OTP")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ApiResponse.ok("OTP sent if the destination is valid");
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ApiResponse<AccountResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.ok(authService.verifyEmail(request), "Email verified");
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with password")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(authService.login(request, ClientContext.from(httpRequest)), "Authenticated");
    }

    @PostMapping("/login/otp")
    @Operation(summary = "Authenticate using mobile OTP")
    public ApiResponse<TokenResponse> loginOtp(
            @Valid @RequestBody LoginOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(authService.loginWithOtp(request, ClientContext.from(httpRequest)), "Authenticated");
    }

    @PostMapping("/oauth/google")
    @Operation(summary = "Authenticate or register with Google")
    public ApiResponse<TokenResponse> google(@Valid @RequestBody OAuthLoginRequest request) {
        TokenResponse tokens = oAuthLoginService.loginWithGoogle(
                request.idToken(),
                request.deviceName(),
                DevicePlatformParser.parse(request.platform())
        );
        return ApiResponse.ok(tokens, "Authenticated");
    }

    @PostMapping("/oauth/apple")
    @Operation(summary = "Authenticate or register with Apple")
    public ApiResponse<TokenResponse> apple(@Valid @RequestBody AppleLoginRequest request) {
        TokenResponse tokens = oAuthLoginService.loginWithApple(
                request.identityToken(),
                request.deviceName(),
                DevicePlatformParser.parse(request.platform())
        );
        return ApiResponse.ok(tokens, "Authenticated");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()), "Token refreshed");
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout current session")
    public ApiResponse<Void> logout() {
        authService.logout(currentUserAccessor.requireUser());
        return ApiResponse.ok("Logged out");
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout all active sessions")
    public ApiResponse<Void> logoutAll() {
        authService.logoutAll(currentUserAccessor.requireUser());
        return ApiResponse.ok("All sessions revoked");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password recovery")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok("If an account exists, recovery instructions were sent");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using recovery token")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset");
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change current password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserAccessor.requireUser(), request);
        return ApiResponse.ok("Password changed");
    }

    @GetMapping("/sessions")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List active sessions")
    public ApiResponse<PagedData<SessionResponse>> sessions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(
                authService.listSessions(currentUserAccessor.requireUser(), PaginationSupport.pageable(page, size)),
                "Sessions"
        );
    }

    @DeleteMapping("/sessions/{sessionId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke a specific session")
    public ApiResponse<Void> revokeSession(@PathVariable Long sessionId) {
        authService.revokeSession(currentUserAccessor.requireUser(), sessionId);
        return ApiResponse.ok("Session revoked");
    }

    @GetMapping("/linked-accounts")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List linked login methods")
    public ApiResponse<LinkedAccountsResponse> linkedAccounts() {
        return ApiResponse.ok(authService.linkedAccounts(currentUserAccessor.requireUser()), "Linked accounts");
    }

    @PostMapping("/linked-accounts/{provider}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Link a Google or Apple account")
    public ApiResponse<Void> linkAccount(
            @PathVariable String provider,
            @Valid @RequestBody LinkAccountRequest request
    ) {
        authService.linkAccount(currentUserAccessor.requireUser(), provider, request);
        return ApiResponse.ok("Linked");
    }

    @DeleteMapping("/linked-accounts/{provider}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Unlink a linked login method")
    public ApiResponse<Void> unlinkAccount(@PathVariable String provider) {
        authService.unlinkAccount(currentUserAccessor.requireUser(), provider);
        return ApiResponse.ok("Unlinked");
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get authenticated account")
    public ApiResponse<AccountResponse> me() {
        return ApiResponse.ok(authService.me(currentUserAccessor.requireUser()), "Account");
    }

    @PostMapping("/deactivate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate account")
    public ApiResponse<Void> deactivate(@RequestBody(required = false) ReasonRequest request) {
        authService.deactivate(currentUserAccessor.requireUser(), request == null ? null : request.reason());
        return ApiResponse.ok("Account deactivated");
    }

    @PostMapping("/reactivate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reactivate a deactivated account")
    public ApiResponse<Void> reactivate() {
        authService.reactivate(currentUserAccessor.requireUser());
        return ApiResponse.ok("Account reactivated");
    }

    @PostMapping("/delete")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Request account deletion")
    public ApiResponse<Void> delete(@Valid @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(currentUserAccessor.requireUser(), request);
        return ApiResponse.ok("Account deleted");
    }

    @PostMapping("/data-export")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Request personal data export")
    public ApiResponse<Map<String, String>> dataExport() {
        return ApiResponse.ok(authService.requestDataExport(currentUserAccessor.requireUser()), "Export requested");
    }
}
