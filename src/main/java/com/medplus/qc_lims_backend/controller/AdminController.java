package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.Right;
import com.medplus.qc_lims_backend.domain.Role;
import com.medplus.qc_lims_backend.domain.User;
import com.medplus.qc_lims_backend.dto.CreateUserRequest;
import com.medplus.qc_lims_backend.dto.UpdateUserRequest;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserManagementService userManagementService;

    public AdminController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    // ============ Users ============

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_USER_READ')")
    public ResponseEntity<List<User>> getUsers(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(userManagementService.findAll(includeInactive));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_USER_CREATE')")
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = User.builder()
                .employeeId(request.getEmployeeId())
                .fullName(request.getFullName())
                .passwordHash(request.getPassword())
                .build();
        User created = userManagementService.create(user);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            userManagementService.assignRoles(created.getUserId(), request.getRoleIds());
            created = userManagementService.findById(created.getUserId());
        }

        created.setPasswordHash(null);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER_UPDATE')")
    public ResponseEntity<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User user = User.builder()
                .userId(id)
                .fullName(request.getFullName())
                .employeeId(request.getEmployeeId())
                .status(request.getStatus())
                .build();
        userManagementService.update(user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_USER_UPDATE')")
    public ResponseEntity<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        userManagementService.assignRoles(id, body.get("roleIds"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_USER_UPDATE')")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userManagementService.updateStatus(id, RecordStatus.valueOf(body.get("status")));
        return ResponseEntity.noContent().build();
    }

    // ============ Roles ============

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ROLE_READ')")
    public ResponseEntity<List<Role>> getRoles(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(userManagementService.findAllRoles(includeInactive));
    }

    @PutMapping("/roles/{roleId}/rights")
    @PreAuthorize("hasAuthority('ROLE_ROLE_UPDATE')")
    public ResponseEntity<Void> assignRightsToRole(@PathVariable Long roleId, @RequestBody Map<String, List<String>> body) {
        userManagementService.assignRightsToRole(roleId, body.get("rightIds"));
        return ResponseEntity.noContent().build();
    }

    // ============ Rights ============

    @GetMapping("/rights")
    @PreAuthorize("hasAuthority('ROLE_ROLE_READ')")
    public ResponseEntity<List<Right>> getRights(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(userManagementService.findAllRights(includeInactive));
    }
}
