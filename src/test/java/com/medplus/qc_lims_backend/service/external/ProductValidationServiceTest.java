package com.medplus.qc_lims_backend.service.external;

import tools.jackson.databind.ObjectMapper;
import com.medplus.qc_lims_backend.dto.BulkRow;
import com.medplus.qc_lims_backend.dto.external.ProductResponseEnvelopeDto;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductValidationServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper;
    private ProductValidationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ProductValidationService(restClient, objectMapper);
    }

    private void mockSuccessfulCall(String innerResponseJson) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        ProductResponseEnvelopeDto envelope = new ProductResponseEnvelopeDto("SUCCESS", null, innerResponseJson, false);
        when(responseSpec.body(eq(ProductResponseEnvelopeDto.class))).thenReturn(envelope);
    }

    @Test
    void testFindActiveProductIds_emptyInput_returnsEmptyWithoutApiCall() {
        Set<String> result = service.findActiveProductIds(Collections.emptySet());
        assertTrue(result.isEmpty());
        verifyNoInteractions(restClient);

        Set<String> nullResult = service.findActiveProductIds(null);
        assertTrue(nullResult.isEmpty());
        verifyNoInteractions(restClient);
    }

    @Test
    void testFindActiveProductIds_successfulRetrieval() {
        String json = "{\"productList\":[{\"productId\":\"ABLU0002\",\"name\":\"ABLUNG\",\"status\":\"ACTIVE\"}],\"totalRows\":1}";
        mockSuccessfulCall(json);

        Set<String> result = service.findActiveProductIds(Set.of("  ABLU0002  "));
        assertEquals(1, result.size());
        assertTrue(result.contains("ABLU0002"));
    }

    @Test
    void testGuardrail1_nullInnerResponse_safelyReturnsEmpty() {
        // External API returns string "null" when no records match
        mockSuccessfulCall("null");

        Set<String> result = service.findActiveProductIds(Set.of("INVALID01"));
        assertTrue(result.isEmpty());
    }

    @Test
    void testGuardrail2_caseInsensitiveMatching() {
        // External API returns uppercase "ABLU0002", Excel user entered lowercase "ablu0002"
        String json = "{\"productList\":[{\"productId\":\"ABLU0002\",\"name\":\"ABLUNG\",\"status\":\"ACTIVE\"}],\"totalRows\":1}";
        mockSuccessfulCall(json);

        Set<String> invalidIds = service.findInvalidProductIds(Set.of("ablu0002"));
        // Since matching is case-insensitive, "ablu0002" should NOT be flagged as invalid
        assertTrue(invalidIds.isEmpty());
    }

    @Test
    void testFindInvalidProductIds_identifiesMissingId() {
        String json = "{\"productList\":[{\"productId\":\"ABLU0002\",\"name\":\"ABLUNG\",\"status\":\"ACTIVE\"}],\"totalRows\":1}";
        mockSuccessfulCall(json);

        Set<String> invalidIds = service.findInvalidProductIds(Set.of("ABLU0002", "MISSING_PROD"));
        assertEquals(1, invalidIds.size());
        assertTrue(invalidIds.contains("MISSING_PROD"));
    }

    @Test
    void testGuardrail4_retryOnTimeout_throwsServiceUnavailable() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(ProductResponseEnvelopeDto.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        assertThrows(ServiceUnavailableException.class, () ->
                service.findActiveProductIds(Set.of("ABLU0002")));

        // Verify it retried 2 times (total 3 attempts)
        verify(restClient, times(3)).post();
    }

    private record DummyBulkRow(Long id, String action, String productId, int rowNumber) implements BulkRow {}

    @Test
    void testValidateProductIdsForBulkRows_dryValidator() {
        String json = "{\"productList\":[{\"productId\":\"ABLU0002\",\"name\":\"ABLUNG\",\"status\":\"ACTIVE\"}],\"totalRows\":1}";
        mockSuccessfulCall(json);

        List<DummyBulkRow> rows = List.of(
                new DummyBulkRow(null, "A", "ABLU0002", 2),
                new DummyBulkRow(null, "A", "BAD_ID", 3)
        );

        Map<Integer, List<String>> errorRows = new HashMap<>();
        service.validateProductIdsForBulkRows(
                rows,
                DummyBulkRow::productId,
                r -> "A".equalsIgnoreCase(r.action()),
                errorRows
        );

        // Row 2 is valid -> no error
        assertNull(errorRows.get(2));
        // Row 3 is invalid -> has error
        assertNotNull(errorRows.get(3));
        assertTrue(errorRows.get(3).contains(ErrorMessages.ERR_PRODUCT_NOT_FOUND_OR_INACTIVE.getMessage()));
    }
}
