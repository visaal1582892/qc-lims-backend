package com.medplus.qc_lims_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String      token;
    private String      tokenType;
    private long        expiresInMs;
    private UserSummary user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long         id;
        private String       employeeId;
        private String       fullName;
        /** Primary role name — used for profile display. */
        private String       role;
        /** All role names the user holds (may be multiple). */
        private List<String> roles;
        /** All role IDs the user holds (parallel to roles). */
        private List<String> roleIds;
        /** Union of rights granted via all roles. */
        private List<String> rights;
    }
}
