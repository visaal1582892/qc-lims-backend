package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.Right;
import com.medplus.qc_lims_backend.domain.Role;
import com.medplus.qc_lims_backend.domain.User;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Rules.md §3.1 — Service interface.
 */
public interface UserManagementServiceInterface {

    // --- Users ---
    Page<User> findAll(boolean includeInactive, Pageable pageable);
    User findById(Long id);
    User create(User user);
    void assignRoles(Long userId, List<Long> roleIds);
    void update(User user);
    void changePassword(Long userId, String currentPassword, String newPassword);
    void updateStatus(Long userId, RecordStatus status);

    // --- Roles ---
    Page<Role> findAllRoles(boolean includeInactive, Pageable pageable);
    void assignRightsToRole(Long roleId, List<String> rightIds);

    // --- Rights ---
    Page<Right> findAllRights(boolean includeInactive, Pageable pageable);
}
