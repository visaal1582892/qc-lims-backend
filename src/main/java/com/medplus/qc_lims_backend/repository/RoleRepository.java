package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.Role;
import com.medplus.qc_lims_backend.domain.Right;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class RoleRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final RightRepository rightRepository;

    public RoleRepository(NamedParameterJdbcTemplate jdbc, RightRepository rightRepository) {
        this.jdbc = jdbc;
        this.rightRepository = rightRepository;
    }

    /** Resolve a list of role IDs to the union of all right_ids via role_rights_mapping. */
    public List<String> findRightIdsByRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptyList();
        return jdbc.queryForList(
                "SELECT DISTINCT rrm.right_id FROM role_rights_mapping rrm " +
                "JOIN roles r ON r.id = rrm.role_id WHERE r.id IN (:ids) AND r.status = 'ACTIVE'",
                new MapSqlParameterSource("ids", roleIds), String.class);
    }

    /** Find role by role_code. */
    public Optional<Role> findByCode(String roleCode) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT id, role_code, role_name, description, status FROM roles WHERE role_code = :code",
                    new MapSqlParameterSource("code", roleCode));
            return Optional.of(mapRole(row));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Find role by id. */
    public Optional<Role> findById(Long id) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT id, role_code, role_name, description, status FROM roles WHERE id = :id",
                    new MapSqlParameterSource("id", id));
            return Optional.of(mapRole(row));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Find role by role_name. */
    public Optional<Role> findByName(String roleName) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT id, role_code, role_name, description, status FROM roles WHERE role_name = :name",
                    new MapSqlParameterSource("name", roleName));
            return Optional.of(mapRole(row));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Find all active roles with rights. */
    public List<Role> findAll(boolean includeInactive) {
        String where = includeInactive ? "" : " WHERE r.status = 'ACTIVE'";
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, role_code, role_name, description, status FROM roles r" + where + " ORDER BY role_name",
                Collections.emptyMap());
        List<Role> roles = rows.stream().map(this::mapRole).toList();
        enrichWithRights(roles);
        return roles;
    }

    private void enrichWithRights(List<Role> roles) {
        if (roles.isEmpty()) return;
        List<Long> ids = roles.stream().map(Role::getId).toList();
        List<Map<String, Object>> mappingRows = jdbc.queryForList(
                "SELECT rrm.role_id, rrm.right_id, ri.right_name, ri.description " +
                "FROM role_rights_mapping rrm JOIN rights ri ON ri.right_id = rrm.right_id " +
                "WHERE rrm.role_id IN (:ids) AND ri.status = 'ACTIVE'",
                new MapSqlParameterSource("ids", ids));

        Map<Long, List<Right>> rightsByRole = new HashMap<>();
        for (Map<String, Object> row : mappingRows) {
            long roleId = ((Number) row.get("role_id")).longValue();
            rightsByRole.computeIfAbsent(roleId, k -> new ArrayList<>()).add(
                    Right.builder()
                            .rightId((String) row.get("right_id"))
                            .rightName((String) row.get("right_name"))
                            .description((String) row.get("description"))
                            .build());
        }
        for (Role role : roles) {
            role.setRights(rightsByRole.getOrDefault(role.getId(), Collections.emptyList()));
        }
    }

    private Role mapRole(Map<String, Object> row) {
        String statusStr = (String) row.get("status");
        RecordStatus status = RecordStatus.ACTIVE;
        if (statusStr != null) {
            try { status = RecordStatus.valueOf(statusStr); } catch (Exception ignored) {}
        }
        return Role.builder()
                .id(((Number) row.get("id")).longValue())
                .roleCode((String) row.get("role_code"))
                .roleName((String) row.get("role_name"))
                .description((String) row.get("description"))
                .status(status)
                .build();
    }
}
