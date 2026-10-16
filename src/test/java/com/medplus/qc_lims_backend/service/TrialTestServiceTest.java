package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.enums.ArStatus;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for TrialTestService — Trial Test and For Information (SRS Module 9).
 *
 * Test matrix:
 *   TT-01: markForInformation on COMPLETED AR — succeeds
 *   TT-02: markForInformation on REGISTERED AR — BusinessRuleViolationException
 *   TT-03: markForInformation on non-existent AR — EntityNotFoundException
 *   TT-04: generateFreshAr from FOR_INFORMATION AR — succeeds with linked reference
 *   TT-05: generateFreshAr from non-FOR_INFORMATION AR — BusinessRuleViolationException
 *   TT-06: generateFreshAr on non-existent AR — EntityNotFoundException
 *   TT-07: getForInfoAr — returns entity
 */
@ExtendWith(MockitoExtension.class)
class TrialTestServiceTest {

    @Mock private SampleInwardRepository sampleInwardRepository;
    @Mock private ArSequenceGenerator arSequenceGenerator;

    @InjectMocks private TrialTestService service;

    private SampleInwardRegister completedAr;

    @BeforeEach
    void setUp() {
        completedAr = new SampleInwardRegister();
        completedAr.setId(1L);
        completedAr.setArNumber("MHSL260001");
        completedAr.setProductId(100);
        completedAr.setBatchNo("BATCH-001");
        completedAr.setReceivedQuantityPacks(10);
        completedAr.setTotalReceivedUnits(100);
        completedAr.setStorageLocation("QC-SH-01");
        completedAr.setArStatus("COMPLETED");
        completedAr.setTestCompletionRate(BigDecimal.valueOf(100));
        completedAr.setIsSubcontracted(false);
        completedAr.setStatus(RecordStatus.ACTIVE);
        completedAr.setCreatedAt(Instant.now());
        completedAr.setUpdatedAt(Instant.now());

        when(sampleInwardRepository.findByArNumberAndStatus("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(java.util.Optional.of(completedAr));
        when(arSequenceGenerator.generateArNumber()).thenReturn("MHSL260002");
        when(sampleInwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- TT-01: Mark For Information ----

    @Test
    void markForInformationOnCompletedSucceeds() {
        SampleInwardRegister result = service.markForInformation("MHSL260001", "Trial test required");

        assertEquals(ArStatus.FOR_INFORMATION.getDbValue(), completedAr.getArStatus());
        verify(sampleInwardRepository).save(completedAr);
    }

    @Test
    void markForInformationOnRegisteredFails() {
        completedAr.setArStatus("REGISTERED");

        assertThrows(BusinessRuleViolationException.class, () ->
                service.markForInformation("MHSL260001", "reason"));
    }

    @Test
    void markForInformationNotFound() {
        when(sampleInwardRepository.findByArNumberAndStatus("NONEXIST", RecordStatus.ACTIVE))
                .thenReturn(java.util.Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                service.markForInformation("NONEXIST", "reason"));
    }

    // ---- TT-02: Generate Fresh AR ----

    @Test
    void generateFreshArFromForInformationSucceeds() {
        completedAr.setArStatus("FOR_INFORMATION");

        SampleInwardRegister fresh = service.generateFreshAr("MHSL260001");

        assertNotNull(fresh);
        assertEquals("MHSL260002", fresh.getArNumber());
        assertEquals("MHSL260001", fresh.getLinkedForInfoArNumber());
        assertEquals(ArStatus.REGISTERED.getDbValue(), fresh.getArStatus());
        assertEquals("BATCH-001", fresh.getBatchNo());

        // Source AR should be marked COMPLETED
        assertEquals(ArStatus.COMPLETED.getDbValue(), completedAr.getArStatus());
        verify(sampleInwardRepository, times(2)).save(any(SampleInwardRegister.class));
    }

    @Test
    void generateFreshArFromNonForInformationFails() {
        completedAr.setArStatus("REGISTERED");

        assertThrows(BusinessRuleViolationException.class, () ->
                service.generateFreshAr("MHSL260001"));
    }

    @Test
    void generateFreshArNotFound() {
        when(sampleInwardRepository.findByArNumberAndStatus("NONEXIST", RecordStatus.ACTIVE))
                .thenReturn(java.util.Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                service.generateFreshAr("NONEXIST"));
    }

    // ---- TT-03: Get For Information AR ----

    @Test
    void getForInfoArSuccess() {
        SampleInwardRegister result = service.getForInfoAr("MHSL260001");

        assertNotNull(result);
        assertEquals("MHSL260001", result.getArNumber());
    }

    @Test
    void getForInfoArNotFound() {
        when(sampleInwardRepository.findByArNumberAndStatus("NONEXIST", RecordStatus.ACTIVE))
                .thenReturn(java.util.Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                service.getForInfoAr("NONEXIST"));
    }
}
