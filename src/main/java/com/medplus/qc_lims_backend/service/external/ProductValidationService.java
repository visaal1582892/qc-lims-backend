package com.medplus.qc_lims_backend.service.external;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.medplus.qc_lims_backend.dto.BulkRow;
import com.medplus.qc_lims_backend.dto.external.ProductItemDto;
import com.medplus.qc_lims_backend.dto.external.ProductListWrapperDto;
import com.medplus.qc_lims_backend.dto.external.ProductRequestDto;
import com.medplus.qc_lims_backend.dto.external.ProductResponseEnvelopeDto;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class ProductValidationService {

    private static final Logger log = LoggerFactory.getLogger(ProductValidationService.class);
    private static final int CHUNK_SIZE = 100;
    private static final int MAX_RETRIES = 2; // total attempts = 3

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProductValidationService(
            @Value("${external.product-service.url:http://192.168.1.211:30021/product/get-products}") String url,
            @Value("${external.product-service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${external.product-service.read-timeout-ms:10000}") int readTimeoutMs,
            @Autowired(required = false) ObjectMapper objectMapper) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(url)
                .build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Package-private constructor for unit testing with a mock/custom RestClient.
     */
    ProductValidationService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a set of product IDs and returns the set of active product IDs found in the product service.
     * Note: Case-insensitive matching is preserved by returning a case-insensitive TreeSet.
     */
    public Set<String> findActiveProductIds(Set<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptySet();
        }

        // Guardrail 2: Strict trimming ONLY, NO uppercase
        List<String> trimmedIds = productIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (trimmedIds.isEmpty()) {
            return Collections.emptySet();
        }

        // Guardrail 3: Chunk into batches of 100 and execute in parallel
        List<List<String>> chunks = partition(trimmedIds, CHUNK_SIZE);

        // Guardrail 2: Store valid IDs in a case-insensitive Set
        Set<String> activeProductIds = Collections.synchronizedSet(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

        chunks.parallelStream().forEach(chunk -> {
            ProductRequestDto requestDto = new ProductRequestDto(new HashSet<>(chunk), "ACTIVE");
            ProductResponseEnvelopeDto envelope = executeWithRetry(requestDto);
            if (envelope != null && !envelope.error() && envelope.response() != null) {
                List<ProductItemDto> items = parseInnerProductList(envelope.response());
                for (ProductItemDto item : items) {
                    if (item.productId() != null && !item.productId().isBlank()) {
                        activeProductIds.add(item.productId().trim());
                    }
                }
            }
        });

        return activeProductIds;
    }

    /**
     * Validates a set of product IDs and returns any input IDs that were NOT returned as active products.
     */
    public Set<String> findInvalidProductIds(Set<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<String> trimmedIds = productIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (trimmedIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> activeIds = findActiveProductIds(new HashSet<>(trimmedIds));

        // Case-insensitive check against active IDs
        Set<String> invalidIds = new HashSet<>();
        for (String id : trimmedIds) {
            if (!activeIds.contains(id)) {
                invalidIds.add(id);
            }
        }
        return invalidIds;
    }

    /**
     * Common generic bulk row validator function supporting the DRY principle across all master pages.
     * Extracts product IDs, checks them in bulk via parallel external calls, and appends error messages
     * to the errorRows map for failed rows.
     *
     * @param rows                    The uploaded bulk rows
     * @param productIdExtractor      Function to extract product ID from row
     * @param shouldValidatePredicate Predicate to determine if row should be validated (e.g. Action A or U with non-blank ID)
     * @param errorRows               The errorRows map collecting errors per rowNumber
     */
    public <T extends BulkRow> void validateProductIdsForBulkRows(
            List<T> rows,
            Function<T, String> productIdExtractor,
            Predicate<T> shouldValidatePredicate,
            Map<Integer, List<String>> errorRows
    ) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        Set<String> candidateIds = new HashSet<>();
        for (T row : rows) {
            if (shouldValidatePredicate.test(row)) {
                String pid = productIdExtractor.apply(row);
                if (pid != null && !pid.trim().isEmpty()) {
                    candidateIds.add(pid.trim());
                }
            }
        }

        if (candidateIds.isEmpty()) {
            return;
        }

        Set<String> invalidIds = findInvalidProductIds(candidateIds);
        if (invalidIds.isEmpty()) {
            return;
        }

        // Case-insensitive lookup for matching against rows
        Set<String> invalidCaseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        invalidCaseInsensitive.addAll(invalidIds);

        for (T row : rows) {
            if (shouldValidatePredicate.test(row)) {
                String pid = productIdExtractor.apply(row);
                if (pid != null && invalidCaseInsensitive.contains(pid.trim())) {
                    List<String> list = errorRows.computeIfAbsent(row.rowNumber(), k -> Collections.synchronizedList(new ArrayList<>()));
                    synchronized (list) {
                        list.add(ErrorMessages.ERR_PRODUCT_NOT_FOUND_OR_INACTIVE.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Executes external POST call with retry (up to MAX_RETRIES) catching timeouts (ResourceAccessException)
     * or 5xx server errors.
     */
    private ProductResponseEnvelopeDto executeWithRetry(ProductRequestDto requestDto) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return restClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestDto)
                        .retrieve()
                        .body(ProductResponseEnvelopeDto.class);
            } catch (ResourceAccessException e) {
                lastException = e;
                log.warn("Network timeout/access error calling Product API (attempt {}/{}): {}",
                        attempt + 1, MAX_RETRIES + 1, e.getMessage());
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().is5xxServerError()) {
                    lastException = e;
                    log.warn("Server 5xx error calling Product API (attempt {}/{}): {}",
                            attempt + 1, MAX_RETRIES + 1, e.getMessage());
                } else {
                    log.error("Client error calling Product API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new ServiceUnavailableException(ErrorMessages.ERR_PRODUCT_SERVICE_UNAVAILABLE.getMessage(), e);
                }
            } catch (RestClientException e) {
                lastException = e;
                log.warn("RestClient error calling Product API (attempt {}/{}): {}",
                        attempt + 1, MAX_RETRIES + 1, e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(150L * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ServiceUnavailableException(ErrorMessages.ERR_PRODUCT_SERVICE_UNAVAILABLE.getMessage(), ie);
                }
            }
        }

        log.error("All {} attempts to call external Product Service failed.", MAX_RETRIES + 1, lastException);
        throw new ServiceUnavailableException(ErrorMessages.ERR_PRODUCT_SERVICE_UNAVAILABLE.getMessage(), lastException);
    }

    /**
     * Guardrail 1: Safely deserializes inner JSON response string into ProductListWrapperDto.
     * Safely returns empty list if string is "null" or blank.
     */
    private List<ProductItemDto> parseInnerProductList(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank() || "null".equalsIgnoreCase(rawResponse.trim())) {
            return Collections.emptyList();
        }
        try {
            ProductListWrapperDto wrapper = objectMapper.readValue(rawResponse, ProductListWrapperDto.class);
            if (wrapper != null && wrapper.productList() != null) {
                return wrapper.productList();
            }
            return Collections.emptyList();
        } catch (JacksonException e) {
            log.error("Failed to parse inner productList JSON: {}", e.getMessage(), e);
            throw new ServiceUnavailableException(ErrorMessages.ERR_PRODUCT_SERVICE_UNAVAILABLE.getMessage(), e);
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
