package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.Right;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RightRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public RightRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Right> findAll(boolean includeInactive) {
        String where = includeInactive ? "" : " WHERE status = 'ACTIVE'";
        return jdbc.query(
                "SELECT right_id, right_name, description FROM rights" + where + " ORDER BY right_name",
                (rs, rowNum) -> Right.builder()
                        .rightId(rs.getString("right_id"))
                        .rightName(rs.getString("right_name"))
                        .description(rs.getString("description"))
                        .build());
    }
}
