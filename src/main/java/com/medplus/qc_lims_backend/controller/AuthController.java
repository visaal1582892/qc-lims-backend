package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.User;
import com.medplus.qc_lims_backend.dto.ChangePasswordRequest;
import com.medplus.qc_lims_backend.dto.LoginRequest;
import com.medplus.qc_lims_backend.dto.LoginResponse;
import com.medplus.qc_lims_backend.security.CustomUserDetails;
import com.medplus.qc_lims_backend.service.AuthService;
import com.medplus.qc_lims_backend.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserManagementService userManagementService;

    public AuthController(AuthService authService, UserManagementService userManagementService) {
        this.authService = authService;
        this.userManagementService = userManagementService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        User u = principal.getUser();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", u.getUserId());
        body.put("employeeId", u.getEmployeeId());
        body.put("fullName", u.getFullName());
        body.put("role", u.getPrimaryRoleName() == null ? "" : u.getPrimaryRoleName());
        body.put("roles", u.getRoleNames() == null ? Collections.emptyList() : u.getRoleNames());
        body.put("roleIds", u.getRoleIds() == null ? Collections.emptyList() : u.getRoleIds());
        body.put("rights", u.getRightNames() == null ? Collections.emptyList() : u.getRightNames());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        userManagementService.changePassword(
                principal.getUser().getUserId(),
                req.getCurrentPassword(),
                req.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
