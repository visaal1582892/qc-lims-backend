package com.medplus.qc_lims_backend.security;

import com.medplus.qc_lims_backend.domain.User;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetails implementation.
 * Authorities are derived from user's rights (rights-based access control).
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<String> rights = user.getRightNames();
        if (rights == null || rights.isEmpty()) {
            return Collections.emptyList();
        }
        return rights.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmployeeId();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() {
        return RecordStatus.ACTIVE == user.getStatus();
    }
}
