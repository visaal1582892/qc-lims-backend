package com.medplus.qc_lims_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generic master-data controller — serves form-dropdown lookups (departments, etc.).
 * Secured via SecurityConfig (GET authenticated, write ops require MASTER_DATA_* rights).
 */
@RestController
@RequestMapping("/api/master")
public class MasterDataController {

    private final NamedParameterJdbcTemplate jdbc;

    public MasterDataController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Departments lookup — used in user forms and sample inward. */
    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getDepartments() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT dept_code AS id, dept_name AS name
                FROM departments WHERE status = 'ACTIVE' ORDER BY dept_name
                """, Collections.emptyMap());
        return ResponseEntity.ok(rows);
    }
}
