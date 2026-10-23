package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.User;
import com.medplus.qc_lims_backend.dto.LoginRequest;
import com.medplus.qc_lims_backend.dto.LoginResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.repository.UserRepository;
import com.medplus.qc_lims_backend.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest req) {
        log.info("LOGIN attempt | employeeId={}", req.getEmployeeId());

        User user = userRepository.findByEmployeeId(req.getEmployeeId())
                .orElseThrow(() -> {
                    log.warn("LOGIN failed — no account | employeeId={}", req.getEmployeeId());
                    return new BadCredentialsException("Invalid employee ID or password");
                });

        if (RecordStatus.ACTIVE != user.getStatus()) {
            log.warn("LOGIN failed — account disabled | employeeId={}", req.getEmployeeId());
            throw new BadCredentialsException("Account is disabled");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("LOGIN failed — wrong password | employeeId={}", req.getEmployeeId());
            throw new BadCredentialsException("Invalid employee ID or password");
        }

        String token = tokenProvider.generateToken(
                user.getUserId(),
                user.getEmployeeId(),
                user.getRoleNames(),
                user.getRightNames(),
                user.getFullName());

        log.info("LOGIN success | employeeId={} userId={} roles={} rights={}",
                user.getEmployeeId(), user.getUserId(), user.getRoleNames(), user.getRightNames());

        LoginResponse.UserSummary summary = LoginResponse.UserSummary.builder()
                .id(user.getUserId())
                .employeeId(user.getEmployeeId())
                .fullName(user.getFullName())
                .role(user.getPrimaryRoleName())
                .roles(user.getRoleNames())
                .roleIds(user.getRoleIds())
                .rights(user.getRightNames())
                .build();

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(expirationMs)
                .user(summary)
                .build();
    }
}
