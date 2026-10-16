package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.dto.request.ArLifecycleRequest;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.enums.ArStatus;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import com.medplus.qc_lims_backend.service.ArSequenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for InwardService AR Lifecycle Management (Phase 5).
 *
 * Test matrix:
 *   T-AR-01: closeAr success (ALL_TESTS_COMPLETE -> COMPLETED)
 *   T-AR-02: closeAr rejected when not ALL_TESTS_COMPLETE
 *   T-AR-03: closeAr rejected for non-existent entity
 *   T-AR-04: cancelAr success from REGISTERED
 *   T-AR-05: cancelAr success from IN_PROGRESS
 *   T-AR-06: cancelAr rejected when ALL_TESTS_COMPLETE
 *   T-AR-07: cancelAr rejected when COMPLETED
 *   T-AR-08: cancelAr rejected for non-existent entity
 *   T-AR-09: updateTestCompletionAndStatus -> REGISTERED (no tests)
 *   T-AR-10: updateTestCompletionAndStatus -> IN_PROGRESS (partial)
 *   T-AR-11: updateTestCompletionAndStatus -> ALL_TESTS_COMPLETE (all terminal)
 *   T-AR-12: updateTestCompletionAndStatus rate calculation (50%)
 *   T-AR-13: updateTestCompletionAndStatus rate calculation (100%)
 *   T-AR-14: updateTestCompletionAndStatus rate calculation (0% — no tests)
 *   T-AR-15: updateTestCompletionAndStatus for non-existent AR (no error)
 */
@ExtendWith(MockitoExtension.class)
class InwardServiceArLifecycleTest {

    @Mock private SampleInwardRepository repository;
    @Mock private ArSequenceGenerator arSequenceGenerator;

    @InjectMocks private InwardService service;

    private SampleInwardRegister sampleInward;

    @BeforeEach
    void setUp() {
        sampleInward = new SampleInwardRegister();
        sampleInward.setId(1L);
        sampleInward.setArNumber("MHSL260001");
        sampleInward.setProductId(100);
        sampleInward.setBatchNo("BATCH-001");
        sampleInward.setReceivedQuantityPacks(10);
        sampleInward.setTotalReceivedUnits(100);
        sampleInward.setIsSubcontracted(false);
        sampleInward.setStatus(RecordStatus.ACTIVE);
    }

    // ==================== closeAr ====================

    @Test
    void closeArSuccessFromAllTestsComplete() {
        // T-AR-01
        sampleInward.setArStatusEnum(ArStatus.ALL_TESTS_COMPLETE);
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("All results verified and approved");

        InwardResponse response = service.closeAr(1L, request, 99L);

        assertEquals("COMPLETED", response.arStatus());
        assertEquals(99L, sampleInward.getUpdatedBy());
        verify(repository).save(sampleInward);
    }

    @Test
    void closeArRejectedWhenRegistered() {
        // T-AR-02
        sampleInward.setArStatusEnum(ArStatus.REGISTERED);
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(sampleInward));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Trying to close");

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.closeAr(1L, request, 99L));
        assertTrue(ex.getMessage().contains("ALL_TESTS_COMPLETE"));
        verify(repository, never()).save(any());
    }

    @Test
    void closeArRejectedWhenInProgress() {
        // T-AR-02 variant
        sampleInward.setArStatusEnum(ArStatus.IN_PROGRESS);
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(sampleInward));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Trying to close");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.closeAr(1L, request, 99L));
    }

    @Test
    void closeArRejectedForNonExistentEntity() {
        // T-AR-03
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.empty());

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Close");

        assertThrows(EntityNotFoundException.class,
                () -> service.closeAr(1L, request, 99L));
    }

    // ==================== cancelAr ====================

    @Test
    void cancelArSuccessFromRegistered() {
        // T-AR-04
        sampleInward.setArStatusEnum(ArStatus.REGISTERED);
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Sample no longer needed");

        InwardResponse response = service.cancelAr(1L, request, 99L);

        assertEquals("CANCELLED", response.arStatus());
        assertEquals(99L, sampleInward.getUpdatedBy());
        verify(repository).save(sampleInward);
    }

    @Test
    void cancelArSuccessFromInProgress() {
        // T-AR-05
        sampleInward.setArStatusEnum(ArStatus.IN_PROGRESS);
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Project cancelled by client");

        InwardResponse response = service.cancelAr(1L, request, 99L);

        assertEquals("CANCELLED", response.arStatus());
    }

    @Test
    void cancelArRejectedWhenAllTestsComplete() {
        // T-AR-06
        sampleInward.setArStatusEnum(ArStatus.ALL_TESTS_COMPLETE);
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(sampleInward));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Cancel");

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.cancelAr(1L, request, 99L));
        assertTrue(ex.getMessage().contains("REGISTERED or IN_PROGRESS"));
    }

    @Test
    void cancelArRejectedWhenCompleted() {
        // T-AR-07
        sampleInward.setArStatusEnum(ArStatus.COMPLETED);
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(sampleInward));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Cancel");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.cancelAr(1L, request, 99L));
    }

    @Test
    void cancelArRejectedForNonExistentEntity() {
        // T-AR-08
        when(repository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.empty());

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Cancel");

        assertThrows(EntityNotFoundException.class,
                () -> service.cancelAr(1L, request, 99L));
    }

    // ==================== updateTestCompletionAndStatus ====================

    @Test
    void updateStatusRegisteredWhenNoTests() {
        // T-AR-09
        when(repository.findByArNumberAndStatusForUpdate("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateTestCompletionAndStatus("MHSL260001", 0, 0);

        ArgumentCaptor<SampleInwardRegister> captor = ArgumentCaptor.forClass(SampleInwardRegister.class);
        verify(repository).save(captor.capture());
        assertEquals(ArStatus.REGISTERED, captor.getValue().getArStatusEnum());
        assertEquals(BigDecimal.ZERO, captor.getValue().getTestCompletionRate());
    }

    @Test
    void updateStatusInProgressWhenPartial() {
        // T-AR-10
        when(repository.findByArNumberAndStatusForUpdate("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateTestCompletionAndStatus("MHSL260001", 4, 2);

        ArgumentCaptor<SampleInwardRegister> captor = ArgumentCaptor.forClass(SampleInwardRegister.class);
        verify(repository).save(captor.capture());
        assertEquals(ArStatus.IN_PROGRESS, captor.getValue().getArStatusEnum());
        assertEquals(new BigDecimal("50.00"), captor.getValue().getTestCompletionRate());
    }

    @Test
    void updateStatusAllTestsCompleteWhenAllTerminal() {
        // T-AR-11
        when(repository.findByArNumberAndStatusForUpdate("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateTestCompletionAndStatus("MHSL260001", 3, 3);

        ArgumentCaptor<SampleInwardRegister> captor = ArgumentCaptor.forClass(SampleInwardRegister.class);
        verify(repository).save(captor.capture());
        assertEquals(ArStatus.ALL_TESTS_COMPLETE, captor.getValue().getArStatusEnum());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getTestCompletionRate());
    }

    @Test
    void updateStatusRateFiftyPercent() {
        // T-AR-12
        when(repository.findByArNumberAndStatusForUpdate("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateTestCompletionAndStatus("MHSL260001", 2, 1);

        ArgumentCaptor<SampleInwardRegister> captor = ArgumentCaptor.forClass(SampleInwardRegister.class);
        verify(repository).save(captor.capture());
        assertEquals(new BigDecimal("50.00"), captor.getValue().getTestCompletionRate());
    }

    @Test
    void updateStatusRateClampedTo100() {
        // T-AR-13
        when(repository.findByArNumberAndStatusForUpdate("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        // Terminal > total edge case
        service.updateTestCompletionAndStatus("MHSL260001", 3, 5);

        ArgumentCaptor<SampleInwardRegister> captor = ArgumentCaptor.forClass(SampleInwardRegister.class);
        verify(repository).save(captor.capture());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getTestCompletionRate());
        assertEquals(ArStatus.ALL_TESTS_COMPLETE, captor.getValue().getArStatusEnum());
    }

    @Test
    void updateStatusZeroRateWhenNoTests() {
        // T-AR-14
        when(repository.findByArNumberAndStatusForUpdate("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(repository.save(any(SampleInwardRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateTestCompletionAndStatus("MHSL260001", 0, 0);

        ArgumentCaptor<SampleInwardRegister> captor = ArgumentCaptor.forClass(SampleInwardRegister.class);
        verify(repository).save(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getValue().getTestCompletionRate());
    }

    @Test
    void updateStatusNoErrorForNonExistentAr() {
        // T-AR-15 — should silently skip, not throw
        when(repository.findByArNumberAndStatusForUpdate("MHSL999", RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.updateTestCompletionAndStatus("MHSL999", 2, 1));
        verify(repository, never()).save(any());
    }
}
