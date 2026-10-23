package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.User;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final RoleRepository roleRepository;

    public UserRepository(NamedParameterJdbcTemplate jdbc, RoleRepository roleRepository) {
        this.jdbc = jdbc;
        this.roleRepository = roleRepository;
    }

    private static final String SELECT_BASE = """
            SELECT u.user_id, u.full_name, u.email, u.password_hash,
                   u.department_id, u.department_name, u.designation_id,
                   u.current_active_tasks, u.status, u.created_at
            FROM users u
            """;

    // ============ Single-user lookups ============

    public Optional<User> findByEmail(String email) {
        try {
            User u = jdbc.queryForObject(
                    SELECT_BASE + " WHERE u.email = :email",
                    new MapSqlParameterSource("email", email),
                    UserRepository::mapRow);
            enrichWithRoles(u);
            return Optional.of(u);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        try {
            User u = jdbc.queryForObject(
                    SELECT_BASE + " WHERE u.user_id = :id",
                    new MapSqlParameterSource("id", id),
                    UserRepository::mapRow);
            enrichWithRoles(u);
            return Optional.of(u);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // ============ Bulk ============

    public List<User> findAll(boolean includeInactive) {
        String where = includeInactive ? "" : " WHERE u.status = 'ACTIVE'";
        List<User> users = jdbc.query(
                SELECT_BASE + where + " ORDER BY u.full_name", UserRepository::mapRow);
        enrichWithRolesBatch(users);
        return users;
    }

    // ============ Write ============

    public Long create(User user) {
        String sql = """
                INSERT INTO users (full_name, email, password_hash, department_id, department_name,
                                   designation_id, current_active_tasks, status)
                VALUES (:fullName, :email, :passwordHash, :departmentId, :departmentName,
                        :designationId, :currentActiveTasks, :status)
                """;
        RecordStatus status = user.getStatus() == null ? RecordStatus.ACTIVE : user.getStatus();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("fullName",           user.getFullName())
                .addValue("email",              user.getEmail())
                .addValue("passwordHash",       user.getPasswordHash())
                .addValue("departmentId",       user.getDepartmentId())
                .addValue("departmentName",     user.getDepartmentName())
                .addValue("designationId",      user.getDesignationId())
                .addValue("currentActiveTasks", user.getCurrentActiveTasks() == null ? 0 : user.getCurrentActiveTasks())
                .addValue("status",             status.name());
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, p, kh, new String[]{"user_id"});
        return kh.getKey() != null ? kh.getKey().longValue() : null;
    }

    public int update(User user) {
        return jdbc.update("""
                UPDATE users SET full_name = :fullName, email = :email,
                    department_id = :departmentId, department_name = :departmentName,
                    designation_id = :designationId, status = :status
                WHERE user_id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("fullName",      user.getFullName())
                        .addValue("email",         user.getEmail())
                        .addValue("departmentId",  user.getDepartmentId())
                        .addValue("departmentName", user.getDepartmentName())
                        .addValue("designationId", user.getDesignationId())
                        .addValue("status",        user.getStatus() == null ? RecordStatus.ACTIVE.name() : user.getStatus().name())
                        .addValue("id",            user.getUserId()));
    }

    public int updatePassword(Long userId, String passwordHash) {
        return jdbc.update(
                "UPDATE users SET password_hash = :hash WHERE user_id = :id",
                new MapSqlParameterSource("hash", passwordHash).addValue("id", userId));
    }

    public int updateStatus(Long userId, RecordStatus status) {
        return jdbc.update(
                "UPDATE users SET status = :status WHERE user_id = :id",
                new MapSqlParameterSource("status", status.name()).addValue("id", userId));
    }

    public int delete(Long userId) {
        return jdbc.update("DELETE FROM users WHERE user_id = :id",
                new MapSqlParameterSource("id", userId));
    }

    // ============ Roles (junction table) ============

    public void insertUserRoles(long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        for (Long roleId : roleIds) {
            jdbc.update("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (:uid, :roleId)",
                    new MapSqlParameterSource("uid", userId).addValue("roleId", roleId));
        }
    }

    public void replaceUserRoles(long userId, List<Long> roleIds) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = :uid",
                new MapSqlParameterSource("uid", userId));
        insertUserRoles(userId, roleIds);
    }

    // ============ Enrichment ============

    private void enrichWithRoles(User user) {
        if (user == null) return;
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT r.id, r.role_code, r.role_name
                  FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                 WHERE ur.user_id = :uid ORDER BY r.role_code
                """, new MapSqlParameterSource("uid", user.getUserId()));

        List<String> roleIds = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();
        List<User.UserRole> roles = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String roleId = row.get("id") != null ? String.valueOf(((Number) row.get("id")).longValue()) : "";
            roleIds.add(roleId);
            roleNames.add((String) row.get("role_name"));
            roles.add(new User.UserRole(
                    roleId.isEmpty() ? null : Long.parseLong(roleId),
                    (String) row.get("role_code"),
                    (String) row.get("role_name")));
        }
        user.setRoleIds(roleIds);
        user.setRoleNames(roleNames);
        user.setRoles(roles);

        // Rights
        List<String> rightIds = roleRepository.findRightIdsByRoleIds(roleIds);
        user.setRightNames(rightIds);
    }

    private void enrichWithRolesBatch(List<User> users) {
        if (users.isEmpty()) return;
        List<Long> ids = users.stream().map(User::getUserId).collect(Collectors.toList());

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT ur.user_id, r.id, r.role_code, r.role_name
                  FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                 WHERE ur.user_id IN (:ids) ORDER BY ur.user_id, r.role_code
                """, new MapSqlParameterSource("ids", ids));

        Map<Long, List<String>> roleIdsMap = new HashMap<>();
        Map<Long, List<String>> roleNamesMap = new HashMap<>();
        Map<Long, List<User.UserRole>> rolesMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            long uid = ((Number) row.get("user_id")).longValue();
            String roleId = String.valueOf(((Number) row.get("id")).longValue());
            roleIdsMap.computeIfAbsent(uid, k -> new ArrayList<>()).add(roleId);
            roleNamesMap.computeIfAbsent(uid, k -> new ArrayList<>()).add((String) row.get("role_name"));
            rolesMap.computeIfAbsent(uid, k -> new ArrayList<>()).add(
                    new User.UserRole(Long.parseLong(roleId),
                            (String) row.get("role_code"),
                            (String) row.get("role_name")));
        }
        for (User u : users) {
            u.setRoleIds(roleIdsMap.getOrDefault(u.getUserId(), Collections.emptyList()));
            u.setRoleNames(roleNamesMap.getOrDefault(u.getUserId(), Collections.emptyList()));
            u.setRoles(rolesMap.getOrDefault(u.getUserId(), Collections.emptyList()));
            u.setRightNames(roleRepository.findRightIdsByRoleIds(u.getRoleIds()));
        }
    }

    // ============ Mapper ============

    private static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static User mapRow(ResultSet rs, int rowNum) throws SQLException {
        String raw = rs.getString("status");
        RecordStatus status = raw == null ? RecordStatus.ACTIVE : RecordStatus.valueOf(raw);
        return User.builder()
                .userId(rs.getLong("user_id"))
                .fullName(rs.getString("full_name"))
                .email(rs.getString("email"))
                .passwordHash(rs.getString("password_hash"))
                .departmentId(rs.getString("department_id"))
                .departmentName(rs.getString("department_name"))
                .designationId(rs.getString("designation_id"))
                .currentActiveTasks(getNullableInt(rs, "current_active_tasks"))
                .status(status)
                .createdAt(rs.getTimestamp("created_at") == null ? null
                        : rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }
}
