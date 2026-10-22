package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.TrfMaster;
import com.medplus.qc_lims_backend.domain.TrfReceiptChecklist;
import com.medplus.qc_lims_backend.dto.request.ReceiptChecklistRequest;
import com.medplus.qc_lims_backend.dto.response.ReceiptChecklistResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.ReceiptChecklistRepository;
import com.medplus.qc_lims_backend.repository.TrfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for physical verification receipt checklists (SRS 3.1 D).
 *
 * Enforces one checklist per TRF (V-20).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptChecklistService {

    private final ReceiptChecklistRepository checklistRepository;
    private final TrfRepository trfRepository;

    @Transactional
    public ReceiptChecklistResponse create(ReceiptChecklistRequest request, Long createdBy) {
        // Verify TRF exists and is ACTIVE
        TrfMaster trf = trfRepository.findByIdAndStatus(request.getTrfId().longValue(), RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("e-TRF", request.getTrfId()));

        // V-20: Exactly one checklist per TRF
        checklistRepository.findByTrfIdAndStatus(request.getTrfId(), RecordStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new BusinessRuleViolationException(
                            "A receipt checklist already exists for TRF " + request.getTrfId() + ".");
                });

        TrfReceiptChecklist entity = new TrfReceiptChecklist();
        entity.setTrfId(request.getTrfId());
        entity.setArNumber(request.getArNumber());
        entity.setMatchSampleNameBatch(request.getMatchSampleNameBatch());
        entity.setFreeFromDamageLeakage(request.getFreeFromDamageLeakage());
        entity.setPharmacopoeialRefMentioned(request.getPharmacopoeialRefMentioned());
        entity.setVerifiedByUserId(request.getVerifiedByUserId());
        entity.setVerifiedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        entity.setStatus(RecordStatus.ACTIVE);

        entity = checklistRepository.save(entity);
        return ReceiptChecklistResponse.fromEntity(entity);
    }

    @Transactional
    public ReceiptChecklistResponse update(Long checklistId, ReceiptChecklistRequest request, Long updatedBy) {
        TrfReceiptChecklist entity = checklistRepository.findByIdAndStatus(checklistId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("ReceiptChecklist", checklistId));

        entity.setMatchSampleNameBatch(request.getMatchSampleNameBatch());
        entity.setFreeFromDamageLeakage(request.getFreeFromDamageLeakage());
        entity.setPharmacopoeialRefMentioned(request.getPharmacopoeialRefMentioned());
        entity.setVerifiedByUserId(request.getVerifiedByUserId());
        entity.setVerifiedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);

        entity = checklistRepository.save(entity);
        return ReceiptChecklistResponse.fromEntity(entity);
    }

    public ReceiptChecklistResponse getByTrfId(Integer trfId) {
        TrfReceiptChecklist entity = checklistRepository.findByTrfIdAndStatus(trfId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("ReceiptChecklist for TRF", trfId));
        return ReceiptChecklistResponse.fromEntity(entity);
    }

    public Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }
}
