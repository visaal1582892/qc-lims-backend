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
        private String       fullName;
        private String       email;
        private String       role;
        private List<String> roles;
        private List<String> roleIds;
        private List<String> rights;
        private String       departmentId;
        private String       department;
        private String       designationId;
        private String       designation;
    }
}
