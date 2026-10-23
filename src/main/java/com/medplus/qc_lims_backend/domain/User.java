package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long          userId;
    private String        employeeId;
    private String        fullName;
    private String        passwordHash;
    private RecordStatus  status;
    private LocalDateTime createdAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserRole {
        private Long   roleId;
        private String roleCode;
        private String roleName;
    }

    @Builder.Default
    private List<String>  roleIds   = Collections.emptyList();
    @Builder.Default
    private List<String>  roleNames = Collections.emptyList();
    @Builder.Default
    private List<UserRole> roles    = Collections.emptyList();
    @Builder.Default
    private List<String>  rightNames = Collections.emptyList();

    public String getPrimaryRoleId() {
        return (roleIds != null && !roleIds.isEmpty()) ? roleIds.get(0) : null;
    }

    public String getPrimaryRoleName() {
        return (roleNames != null && !roleNames.isEmpty()) ? roleNames.get(0) : null;
    }

    public boolean hasRole(String roleName) {
        if (roleNames == null || roleName == null) return false;
        return roleNames.stream().anyMatch(n -> n.equalsIgnoreCase(roleName));
    }

    public boolean hasAuthority(String authority) {
        if (rightNames == null || authority == null) return false;
        return rightNames.stream().anyMatch(r -> r.equalsIgnoreCase(authority));
    }
}
