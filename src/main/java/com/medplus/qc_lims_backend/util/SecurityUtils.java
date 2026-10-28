package com.medplus.qc_lims_backend.util;

import com.medplus.qc_lims_backend.security.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * Utility to resolve current authenticated user/employee ID for audit and logging.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentEmployeeId() {
        return getCurrentEmployeeId("SYSTEM");
    }

    public static String getCurrentEmployeeId(String fallback) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof CustomUserDetails cud) {
                String empId = cud.getUser().getEmployeeId();
                if (StringUtils.hasText(empId)) {
                    return empId;
                }
            }
            if (StringUtils.hasText(auth.getName()) && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        }
        return StringUtils.hasText(fallback) ? fallback : "SYSTEM";
    }
}
