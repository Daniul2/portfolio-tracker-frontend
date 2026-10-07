package com.kodilla.portfolio.ui.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodilla.portfolio.ui.client.BackendDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** The frontend's only route to the backend: every screen goes through here. */
@Component
public class PortfolioApiClient {

    private static final Logger log = LoggerFactory.getLogger(PortfolioApiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Long userId;

    public PortfolioApiClient(RestClient backendRestClient,
                              ObjectMapper objectMapper,
                              @Value("${app.backend.user-id}") Long userId) {
        this.restClient = backendRestClient;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Long currentUserId() {
        return userId;
    }

    // --- portfolios -----------------------------------------------------

    public List<Portfolio> portfolios() {
        return call(() -> restClient.get()
                .uri(uri -> uri.path("/portfolios").queryParam("userId", userId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Portfolio>>() {
                }));
    }

    public PortfolioSummary summary(Long portfolioId) {
        return call(() -> restClient.get()
                .uri("/portfolios/{id}/summary", portfolioId)
                .retrieve()
                .body(PortfolioSummary.class));
    }

    public Portfolio createPortfolio(PortfolioRequest request) {
        return call(() -> restClient.post()
                .uri("/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Portfolio.class));
    }

    public void deletePortfolio(Long portfolioId) {
        call(() -> restClient.delete()
                .uri("/portfolios/{id}", portfolioId)
                .retrieve()
                .toBodilessEntity());
    }

    // --- assets ---------------------------------------------------------

    public List<Asset> assets() {
        return call(() -> restClient.get()
                .uri("/assets")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Asset>>() {
                }));
    }

    public Asset createAsset(AssetRequest request) {
        return call(() -> restClient.post()
                .uri("/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Asset.class));
    }

    public Asset setAssetActive(Long assetId, boolean active) {
        return call(() -> restClient.patch()
                .uri(uri -> uri.path("/assets/{id}/active").queryParam("value", active).build(assetId))
                .retrieve()
                .body(Asset.class));
    }

    public void deleteAsset(Long assetId) {
        call(() -> restClient.delete()
                .uri("/assets/{id}", assetId)
                .retrieve()
                .toBodilessEntity());
    }

    // --- transactions ---------------------------------------------------

    public List<Transaction> transactions(Long portfolioId) {
        return call(() -> restClient.get()
                .uri(uri -> uri.path("/transactions").queryParam("portfolioId", portfolioId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Transaction>>() {
                }));
    }

    public Transaction createTransaction(TransactionRequest request) {
        return call(() -> restClient.post()
                .uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Transaction.class));
    }

    public Transaction updateTransaction(Long id, TransactionRequest request) {
        return call(() -> restClient.put()
                .uri("/transactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Transaction.class));
    }

    public void deleteTransaction(Long id) {
        call(() -> restClient.delete()
                .uri("/transactions/{id}", id)
                .retrieve()
                .toBodilessEntity());
    }

    // --- alerts ---------------------------------------------------------

    public List<Alert> alerts(Long portfolioId) {
        return call(() -> restClient.get()
                .uri(uri -> uri.path("/alerts").queryParam("portfolioId", portfolioId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Alert>>() {
                }));
    }

    public Alert createAlert(AlertRequest request) {
        return call(() -> restClient.post()
                .uri("/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Alert.class));
    }

    public Alert updateAlert(Long id, AlertRequest request) {
        return call(() -> restClient.put()
                .uri("/alerts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Alert.class));
    }

    public void deleteAlert(Long id) {
        call(() -> restClient.delete()
                .uri("/alerts/{id}", id)
                .retrieve()
                .toBodilessEntity());
    }

    public List<AlertEvent> alertEvents(Long portfolioId) {
        return call(() -> restClient.get()
                .uri(uri -> uri.path("/alerts/events").queryParam("portfolioId", portfolioId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<AlertEvent>>() {
                }));
    }

    public AlertEvent acknowledgeEvent(Long eventId) {
        return call(() -> restClient.put()
                .uri("/alerts/events/{id}/acknowledge", eventId)
                .retrieve()
                .body(AlertEvent.class));
    }

    public int evaluateAlerts() {
        Map<String, Object> result = call(() -> restClient.post()
                .uri("/alerts/evaluate")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
        Object count = result == null ? null : result.get("count");
        return count instanceof Number number ? number.intValue() : 0;
    }

    // --- market data ----------------------------------------------------

    public List<PriceSnapshot> latestPrices() {
        return call(() -> restClient.get()
                .uri("/prices/latest")
                .retrieve()
                .body(new ParameterizedTypeReference<List<PriceSnapshot>>() {
                }));
    }

    public List<ExchangeRate> rates() {
        return call(() -> restClient.get()
                .uri("/rates")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ExchangeRate>>() {
                }));
    }

    public BigDecimal usdRate(String target) {
        Map<String, Object> body = call(() -> restClient.get()
                .uri(uri -> uri.path("/rates/convert").queryParam("to", target).build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
        Object rate = body == null ? null : body.get("rate");
        return rate == null ? null : new BigDecimal(rate.toString());
    }

    /** Refreshes both external sources and re-runs the alert rules. */
    public MarketRefreshResult refreshMarketData() {
        return call(() -> restClient.post()
                .uri("/dashboard/refresh")
                .retrieve()
                .body(MarketRefreshResult.class));
    }

    // --- error translation ----------------------------------------------

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new BackendException(e.getStatusCode().value(), describe(e));
        } catch (ResourceAccessException e) {
            log.warn("Backend unreachable: {}", e.getMessage());
            throw new BackendException(
                    "Cannot reach the backend. Is it running? " + e.getMessage(), e);
        }
    }

    /**
     * Pulls the human-readable message out of the API's error body, falling
     * back to the raw status when the body is not the expected shape.
     */
    private String describe(RestClientResponseException e) {
        try {
            ApiError error = objectMapper.readValue(e.getResponseBodyAsString(), ApiError.class);
            if (error.fieldErrors() != null && !error.fieldErrors().isEmpty()) {
                String fields = error.fieldErrors().entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("");
                return error.message() + " (" + fields + ")";
            }
            return error.message();
        } catch (Exception ignored) {
            return "Backend returned " + e.getStatusCode().value();
        }
    }
}
