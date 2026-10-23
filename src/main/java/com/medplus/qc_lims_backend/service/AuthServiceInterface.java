package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.LoginRequest;
import com.medplus.qc_lims_backend.dto.LoginResponse;

/**
 * Rules.md §3.1 — Service interface.
 */
public interface AuthServiceInterface {
    LoginResponse login(LoginRequest request);
}
