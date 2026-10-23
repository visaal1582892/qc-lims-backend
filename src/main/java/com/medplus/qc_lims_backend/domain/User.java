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
    private String        fullName;
    private String        email;
    private String        passwordHash;
    private String        departmentId;
    private String        departmentName;
    private String        designationId;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserRole {
        private Long   id;
        private String roleCode;
        private String roleName;
    }

    @Builder.Default
    private List<String>  roleIds   = Collections.emptyList();
    @Builder.Default
    private List<String>  roleNames = Collections.emptyList();
    @Builder.Default
    private List<UserRole> roles = Collections.emptyList();
    @Builder.Default
    private List<String>  rightNames = Collections.emptyList();
    @Builder.Default
    private List<String>  capabilityIds = Collections.emptyList();
    @Builder.Default
    private List<String>  capabilityNames = Collections.emptyList();
    private Integer       currentActiveTasks;
    private RecordStatus  status;
    private LocalDateTime createdAt;

    public String getPrimaryRoleId() {
        return (roleIds != null && !roleIds.isEmpty()) ? roleIds.get(0) : null;
    }

    public String getPrimaryRoleName() {
        return (roleNames != null && !roleNames.isEmpty()) ? roleNames.get(0) : null;
    }

    public boolean hasRole(String logicalRoleName) {
        if (roleNames == null || logicalRoleName == null) return false;
        return roleNames.stream().anyMatch(name -> name.equalsIgnoreCase(logicalRoleName));
    }

    public boolean hasAuthority(String authority) {
        if (rightNames == null || authority == null) return false;
        return rightNames.stream().anyMatch(r -> r.equalsIgnoreCase(authority));
    }
}
