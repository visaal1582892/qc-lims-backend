package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.Right;
import com.medplus.qc_lims_backend.domain.Role;
import com.medplus.qc_lims_backend.domain.User;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.exception.ResourceNotFoundException;
import com.medplus.qc_lims_backend.repository.RightRepository;
import com.medplus.qc_lims_backend.repository.RoleRepository;
import com.medplus.qc_lims_backend.repository.UserRepository;
import com.medplus.qc_lims_backend.service.UserManagementServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class UserManagementServiceImpl implements UserManagementServiceInterface {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RightRepository rightRepository;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbc;

    public UserManagementServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     RightRepository rightRepository,
                                     PasswordEncoder passwordEncoder,
                                     NamedParameterJdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.rightRepository = rightRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    // ============ Users ============

    @Override
    public Page<User> findAll(boolean includeInactive, Pageable pageable) {
        List<User> all = userRepository.findAll(includeInactive);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<User> pageContent = start < all.size() ? all.subList(start, end) : List.of();
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    @Transactional
    public User create(User user) {
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        Long newId = userRepository.create(user);
        user.setUserId(newId);
        log.info("Created user {} (id={})", user.getEmployeeId(), newId);
        return userRepository.findById(newId).orElse(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userRepository.replaceUserRoles(userId, roleIds != null ? roleIds : List.of());
        log.info("Assigned roles {} to user {}", roleIds, userId);
    }

    @Override
    @Transactional
    public void update(User user) {
        userRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + user.getUserId()));
        userRepository.update(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        userRepository.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, RecordStatus status) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userRepository.updateStatus(userId, status);
    }

    // ============ Roles ============

    @Override
    public Page<Role> findAllRoles(boolean includeInactive, Pageable pageable) {
        List<Role> all = roleRepository.findAll(includeInactive);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Role> pageContent = start < all.size() ? all.subList(start, end) : List.of();
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    @Override
    @Transactional
    public void assignRightsToRole(Long roleId, List<String> rightIds) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        String createdBy = com.medplus.qc_lims_backend.util.SecurityUtils.getCurrentEmployeeId();
        jdbc.update("""
                INSERT INTO role_rights_log (role_id, right_id, created_by)
                SELECT role_id, right_id, :createdBy
                FROM role_rights WHERE role_id = :rid
                """,
                new MapSqlParameterSource("rid", roleId).addValue("createdBy", createdBy));

        jdbc.update("DELETE FROM role_rights WHERE role_id = :rid",
                new MapSqlParameterSource("rid", roleId));
        if (rightIds != null) {
            for (String rightId : rightIds) {
                jdbc.update("INSERT INTO role_rights (role_id, right_id) VALUES (:rid, :rightId)",
                        new MapSqlParameterSource("rid", roleId).addValue("rightId", rightId));
            }
        }
    }

    // ============ Rights ============

    @Override
    public Page<Right> findAllRights(boolean includeInactive, Pageable pageable) {
        List<Right> all = rightRepository.findAll(includeInactive);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Right> pageContent = start < all.size() ? all.subList(start, end) : List.of();
        return new PageImpl<>(pageContent, pageable, all.size());
    }
}
