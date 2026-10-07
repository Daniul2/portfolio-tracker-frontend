package com.kodilla.portfolio.ui.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Mirrors of the backend's response shapes. */
public final class BackendDtos {

    private BackendDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(Long id, String username, String email, String displayName,
                       LocalDateTime createdAt, int portfolioCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Portfolio(Long id, Long userId, String name, String baseCurrency,
                            LocalDateTime createdAt, int transactionCount) {
        @Override
        public String toString() {
            return name == null ? "" : name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asset(Long id, String externalId, String symbol, String name,
                        boolean active, LocalDateTime createdAt) {
        @Override
        public String toString() {
            return symbol == null ? "" : symbol + " - " + name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Holding(Long assetId, String symbol, String assetName,
                          BigDecimal quantity, BigDecimal averageCostUsd, BigDecimal costBasisUsd,
                          BigDecimal currentPriceUsd, BigDecimal marketValueUsd,
                          BigDecimal unrealizedPnlUsd, BigDecimal unrealizedPnlPercent,
                          BigDecimal marketValueBase, boolean priced) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PortfolioSummary(Long portfolioId, String portfolioName, String baseCurrency,
                                   BigDecimal totalCostUsd, BigDecimal totalValueUsd,
                                   BigDecimal totalPnlUsd, BigDecimal totalPnlPercent,
                                   BigDecimal fxRate, BigDecimal totalValueBase,
                                   boolean fullyPriced,
                                   List<Holding> holdings, LocalDateTime valuedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(Long id, Long portfolioId, Long assetId, String symbol,
                              String type, BigDecimal quantity, BigDecimal pricePerUnitUsd,
                              BigDecimal feeUsd, BigDecimal grossValueUsd,
                              LocalDateTime executedAt, String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(Long id, Long portfolioId, Long assetId, String symbol, String type,
                        BigDecimal threshold, boolean active,
                        LocalDateTime createdAt, LocalDateTime lastTriggeredAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlertEvent(Long id, Long alertId, Long portfolioId, String message,
                             BigDecimal valueAtTrigger, boolean acknowledged,
                             LocalDateTime createdAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceSnapshot(Long id, Long assetId, String symbol, BigDecimal priceUsd,
                                BigDecimal change24hPercent, LocalDateTime capturedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExchangeRate(Long id, String currencyCode, BigDecimal ratePln,
                               LocalDate effectiveDate, LocalDateTime fetchedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MarketRefreshResult(int pricesSaved, int ratesSaved, int alertsTriggered,
                                      List<String> failures, LocalDateTime completedAt) {
    }

    /** The uniform error body every backend failure uses. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(int status, String error, String message,
                           java.util.Map<String, String> fieldErrors, LocalDateTime timestamp) {
    }

    // --- request bodies -------------------------------------------------

    public record TransactionRequest(Long portfolioId, Long assetId, String type,
                                     BigDecimal quantity, BigDecimal pricePerUnitUsd,
                                     BigDecimal feeUsd, LocalDateTime executedAt, String note) {
    }

    public record AlertRequest(Long portfolioId, Long assetId, String type,
                               BigDecimal threshold, Boolean active) {
    }

    public record AssetRequest(String externalId, String symbol, String name) {
    }

    public record PortfolioRequest(Long userId, String name, String baseCurrency) {
    }
}
