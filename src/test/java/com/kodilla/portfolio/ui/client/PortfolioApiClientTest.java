package com.kodilla.portfolio.ui.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kodilla.portfolio.ui.client.BackendDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Exercises the frontend's only route to the backend against a stubbed HTTP server, so no backend
 * needs to be running.
 */
class PortfolioApiClientTest {

    private MockRestServiceServer server;
    private PortfolioApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://backend.test/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        // The backend sends ISO date-times, so the mapper needs the JSR-310 module
        // exactly as Spring Boot configures it in the running application.
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        client = new PortfolioApiClient(builder.build(), objectMapper, 1L);
    }

    @Test
    @DisplayName("exposes the configured user id")
    void exposesUserId() {
        assertThat(client.currentUserId()).isEqualTo(1L);
    }

    @Nested
    @DisplayName("reading data")
    class Reading {

        @Test
        @DisplayName("portfolios are requested for the configured user and mapped")
        void mapsPortfolios() {
            String body = """
                    [{"id":1,"userId":1,"name":"Long term","baseCurrency":"PLN",
                      "createdAt":"2026-07-30T08:33:28","transactionCount":4}]
                    """;
            server.expect(requestTo(org.hamcrest.Matchers.containsString("/portfolios")))
                    .andExpect(queryParam("userId", "1"))
                    .andExpect(method(org.springframework.http.HttpMethod.GET))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            List<Portfolio> portfolios = client.portfolios();

            server.verify();
            assertThat(portfolios).hasSize(1);
            assertThat(portfolios.get(0).name()).isEqualTo("Long term");
            assertThat(portfolios.get(0).baseCurrency()).isEqualTo("PLN");
            assertThat(portfolios.get(0).transactionCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("a portfolio renders as its name in a combo box")
        void portfolioToStringIsItsName() {
            assertThat(new Portfolio(1L, 1L, "Long term", "PLN", null, 0)).hasToString("Long term");
        }

        @Test
        @DisplayName("the summary maps both the USD figures and the converted total")
        void mapsSummary() {
            String body = """
                    {"portfolioId":1,"portfolioName":"Long term","baseCurrency":"PLN",
                     "totalCostUsd":21550.00,"totalValueUsd":27032.20,"totalPnlUsd":5482.20,
                     "totalPnlPercent":25.44,"fxRate":3.7644,"totalValueBase":101760.01,
                     "holdings":[{"assetId":1,"symbol":"BTC","assetName":"Bitcoin",
                       "quantity":0.35,"averageCostUsd":46571.43,"costBasisUsd":16300.00,
                       "currentPriceUsd":63749.00,"marketValueUsd":22312.15,
                       "unrealizedPnlUsd":6012.15,"unrealizedPnlPercent":36.88,
                       "marketValueBase":83991.86,"priced":true}],
                     "valuedAt":"2026-07-31T11:21:07"}
                    """;
            server.expect(requestTo("http://backend.test/v1/portfolios/1/summary"))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            PortfolioSummary summary = client.summary(1L);

            assertThat(summary.totalValueUsd()).isEqualByComparingTo("27032.20");
            assertThat(summary.fxRate()).isEqualByComparingTo("3.7644");
            assertThat(summary.totalValueBase()).isEqualByComparingTo("101760.01");
            assertThat(summary.holdings()).hasSize(1);
            assertThat(summary.holdings().get(0).symbol()).isEqualTo("BTC");
            assertThat(summary.holdings().get(0).priced()).isTrue();
            assertThat(summary.valuedAt()).isNotNull();
        }

        @Test
        @DisplayName("an unpriced holding keeps its null figures rather than defaulting to zero")
        void keepsNullsForUnpricedHolding() {
            String body = """
                    {"portfolioId":1,"portfolioName":"P","baseCurrency":"PLN",
                     "totalCostUsd":100,"totalValueUsd":0,"totalPnlUsd":0,"totalPnlPercent":null,
                     "fxRate":null,"totalValueBase":null,
                     "holdings":[{"assetId":2,"symbol":"ETH","assetName":"Ethereum",
                       "quantity":1,"averageCostUsd":100,"costBasisUsd":100,
                       "currentPriceUsd":null,"marketValueUsd":null,"unrealizedPnlUsd":null,
                       "unrealizedPnlPercent":null,"marketValueBase":null,"priced":false}],
                     "valuedAt":"2026-07-31T11:21:07"}
                    """;
            server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            PortfolioSummary summary = client.summary(1L);

            assertThat(summary.fxRate()).isNull();
            assertThat(summary.totalValueBase()).isNull();
            assertThat(summary.holdings().get(0).priced()).isFalse();
            assertThat(summary.holdings().get(0).marketValueUsd()).isNull();
        }

        @Test
        @DisplayName("assets are mapped and render as symbol plus name")
        void mapsAssets() {
            String body = """
                    [{"id":1,"externalId":"bitcoin","symbol":"BTC","name":"Bitcoin",
                      "active":true,"createdAt":"2026-07-30T08:33:28"}]
                    """;
            server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            List<Asset> assets = client.assets();

            assertThat(assets).hasSize(1);
            assertThat(assets.get(0).externalId()).isEqualTo("bitcoin");
            assertThat(assets.get(0)).hasToString("BTC - Bitcoin");
        }

        @Test
        @DisplayName("transactions are requested for one portfolio")
        void mapsTransactions() {
            String body = """
                    [{"id":1,"portfolioId":1,"assetId":1,"symbol":"BTC","type":"BUY",
                      "quantity":0.25,"pricePerUnitUsd":42000,"feeUsd":0,"grossValueUsd":10500,
                      "executedAt":"2026-04-01T08:33:00","note":null}]
                    """;
            server.expect(queryParam("portfolioId", "1"))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            List<Transaction> transactions = client.transactions(1L);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).type()).isEqualTo("BUY");
            assertThat(transactions.get(0).grossValueUsd()).isEqualByComparingTo("10500");
        }

        @Test
        @DisplayName("alerts and their events are mapped")
        void mapsAlertsAndEvents() {
            server.expect(anything()).andRespond(withSuccess("""
                    [{"id":1,"portfolioId":1,"assetId":1,"symbol":"BTC","type":"PRICE_ABOVE",
                      "threshold":70000,"active":true,"createdAt":"2026-07-30T08:33:28",
                      "lastTriggeredAt":null}]
                    """, MediaType.APPLICATION_JSON));

            List<Alert> alerts = client.alerts(1L);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).type()).isEqualTo("PRICE_ABOVE");
            assertThat(alerts.get(0).active()).isTrue();
            assertThat(alerts.get(0).lastTriggeredAt()).isNull();
        }

        @Test
        @DisplayName("latest prices and exchange rates are mapped")
        void mapsMarketData() {
            server.expect(anything()).andRespond(withSuccess("""
                    [{"id":1,"assetId":1,"symbol":"BTC","priceUsd":63749.00,
                      "change24hPercent":-0.70,"capturedAt":"2026-07-31T11:11:10"}]
                    """, MediaType.APPLICATION_JSON));
            List<PriceSnapshot> prices = client.latestPrices();
            assertThat(prices.get(0).priceUsd()).isEqualByComparingTo("63749.00");

            server.reset();
            server.expect(anything()).andRespond(withSuccess("""
                    [{"id":1,"currencyCode":"USD","ratePln":3.7644,
                      "effectiveDate":"2026-07-31","fetchedAt":"2026-07-31T11:11:10"}]
                    """, MediaType.APPLICATION_JSON));
            List<ExchangeRate> rates = client.rates();
            assertThat(rates.get(0).currencyCode()).isEqualTo("USD");
            assertThat(rates.get(0).effectiveDate()).isNotNull();
        }

        @Test
        @DisplayName("the USD conversion rate is pulled out of the response map")
        void readsConversionRate() {
            server.expect(queryParam("to", "PLN"))
                    .andRespond(withSuccess("{\"from\":\"USD\",\"to\":\"PLN\",\"rate\":3.7644}",
                            MediaType.APPLICATION_JSON));

            assertThat(client.usdRate("PLN")).isEqualByComparingTo("3.7644");
        }

        @Test
        @DisplayName("a missing rate in the response yields null rather than throwing")
        void handlesMissingRate() {
            server.expect(anything())
                    .andRespond(withSuccess("{\"from\":\"USD\",\"to\":\"PLN\"}",
                            MediaType.APPLICATION_JSON));

            assertThat(client.usdRate("PLN")).isNull();
        }
    }

    @Nested
    @DisplayName("writing data")
    class Writing {

        @Test
        @DisplayName("creating a portfolio posts the configured user's id and maps the result")
        void createsPortfolio() {
            server.expect(method(org.springframework.http.HttpMethod.POST))
                    .andExpect(requestTo("http://backend.test/v1/portfolios"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                            .jsonPath("$.userId").value(1))
                    .andRespond(withSuccess("""
                            {"id":5,"userId":1,"name":"Savings","baseCurrency":"EUR",
                             "createdAt":"2026-10-07T10:00:00","transactionCount":0}
                            """, MediaType.APPLICATION_JSON));

            Portfolio created = client.createPortfolio(
                    new PortfolioRequest(client.currentUserId(), "Savings", "EUR"));

            server.verify();
            assertThat(created.id()).isEqualTo(5L);
            assertThat(created.baseCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("deleting a portfolio issues DELETE")
        void deletesPortfolio() {
            server.expect(requestTo("http://backend.test/v1/portfolios/5"))
                    .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                    .andRespond(withNoContent());

            client.deletePortfolio(5L);

            server.verify();
        }

        @Test
        @DisplayName("creating a transaction posts JSON and maps the result")
        void createsTransaction() {
            server.expect(method(org.springframework.http.HttpMethod.POST))
                    .andExpect(header("Content-Type", "application/json"))
                    .andRespond(withSuccess("""
                            {"id":9,"portfolioId":1,"assetId":1,"symbol":"BTC","type":"BUY",
                             "quantity":1,"pricePerUnitUsd":50000,"feeUsd":0,"grossValueUsd":50000,
                             "executedAt":"2026-07-31T10:00:00","note":"test"}
                            """, MediaType.APPLICATION_JSON));

            Transaction created = client.createTransaction(new TransactionRequest(
                    1L, 1L, "BUY", BigDecimal.ONE, new BigDecimal("50000"),
                    BigDecimal.ZERO, null, "test"));

            server.verify();
            assertThat(created.id()).isEqualTo(9L);
        }

        @Test
        @DisplayName("deleting a transaction issues DELETE and needs no response body")
        void deletesTransaction() {
            server.expect(method(org.springframework.http.HttpMethod.DELETE))
                    .andRespond(withNoContent());

            client.deleteTransaction(9L);

            server.verify();
        }

        @Test
        @DisplayName("updating an alert issues PUT")
        void updatesAlert() {
            server.expect(method(org.springframework.http.HttpMethod.PUT))
                    .andRespond(withSuccess("""
                            {"id":1,"portfolioId":1,"assetId":1,"symbol":"BTC","type":"PRICE_ABOVE",
                             "threshold":80000,"active":false,"createdAt":"2026-07-30T08:33:28",
                             "lastTriggeredAt":null}
                            """, MediaType.APPLICATION_JSON));

            Alert updated = client.updateAlert(1L,
                    new AlertRequest(1L, 1L, "PRICE_ABOVE", new BigDecimal("80000"), false));

            server.verify();
            assertThat(updated.active()).isFalse();
        }

        @Test
        @DisplayName("toggling an asset is a PATCH with the flag as a query parameter")
        void togglesAsset() {
            server.expect(queryParam("value", "false"))
                    .andExpect(method(org.springframework.http.HttpMethod.PATCH))
                    .andRespond(withSuccess("""
                            {"id":1,"externalId":"bitcoin","symbol":"BTC","name":"Bitcoin",
                             "active":false,"createdAt":"2026-07-30T08:33:28"}
                            """, MediaType.APPLICATION_JSON));

            assertThat(client.setAssetActive(1L, false).active()).isFalse();
        }

        @Test
        @DisplayName("evaluating alerts reads the count out of the response")
        void readsTriggeredCount() {
            server.expect(method(org.springframework.http.HttpMethod.POST))
                    .andRespond(withSuccess("{\"label\":\"alertsTriggered\",\"count\":3}",
                            MediaType.APPLICATION_JSON));

            assertThat(client.evaluateAlerts()).isEqualTo(3);
        }

        @Test
        @DisplayName("a malformed evaluate response counts as zero rather than throwing")
        void handlesMissingCount() {
            server.expect(anything())
                    .andRespond(withSuccess("{\"label\":\"alertsTriggered\"}",
                            MediaType.APPLICATION_JSON));

            assertThat(client.evaluateAlerts()).isZero();
        }

        @Test
        @DisplayName("a market refresh reports both providers and any failures")
        void refreshesMarketData() {
            server.expect(method(org.springframework.http.HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"pricesSaved":4,"ratesSaved":32,"alertsTriggered":1,
                             "failures":[],"completedAt":"2026-07-31T11:11:12"}
                            """, MediaType.APPLICATION_JSON));

            MarketRefreshResult result = client.refreshMarketData();

            assertThat(result.pricesSaved()).isEqualTo(4);
            assertThat(result.ratesSaved()).isEqualTo(32);
            assertThat(result.failures()).isEmpty();
        }

        @Test
        @DisplayName("a partial refresh failure is carried through, not swallowed")
        void carriesPartialFailure() {
            server.expect(anything()).andRespond(withSuccess("""
                    {"pricesSaved":0,"ratesSaved":32,"alertsTriggered":0,
                     "failures":["CoinGecko: timed out"],"completedAt":"2026-07-31T11:11:12"}
                    """, MediaType.APPLICATION_JSON));

            assertThat(client.refreshMarketData().failures())
                    .containsExactly("CoinGecko: timed out");
        }
    }

    @Nested
    @DisplayName("turning failures into readable messages")
    class ErrorHandling {

        @Test
        @DisplayName("a 404 surfaces the backend's own message")
        void translatesNotFound() {
            server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"status":404,"error":"Not Found",
                             "message":"Portfolio with id 99 was not found",
                             "fieldErrors":{},"timestamp":"2026-07-31T11:00:00"}
                            """));

            assertThatThrownBy(() -> client.summary(99L))
                    .isInstanceOf(BackendException.class)
                    .hasMessage("Portfolio with id 99 was not found");
        }

        @Test
        @DisplayName("a business-rule rejection keeps its explanation")
        void translatesBusinessRule() {
            server.expect(anything()).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"status":422,"error":"Unprocessable Entity",
                             "message":"Cannot sell 99 BTC: portfolio only holds 0.35000000",
                             "fieldErrors":{},"timestamp":"2026-07-31T11:00:00"}
                            """));

            assertThatThrownBy(() -> client.createTransaction(new TransactionRequest(
                    1L, 1L, "SELL", new BigDecimal("99"), new BigDecimal("60000"),
                    null, null, null)))
                    .isInstanceOf(BackendException.class)
                    .hasMessageContaining("only holds");
        }

        @Test
        @DisplayName("validation errors are expanded field by field")
        void expandsFieldErrors() {
            server.expect(anything()).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"status":400,"error":"Bad Request",
                             "message":"Request validation failed",
                             "fieldErrors":{"quantity":"quantity must be greater than zero"},
                             "timestamp":"2026-07-31T11:00:00"}
                            """));

            assertThatThrownBy(() -> client.createTransaction(new TransactionRequest(
                    1L, 1L, "BUY", BigDecimal.ZERO, BigDecimal.TEN, null, null, null)))
                    .isInstanceOf(BackendException.class)
                    .hasMessageContaining("Request validation failed")
                    .hasMessageContaining("quantity must be greater than zero");
        }

        @Test
        @DisplayName("a response that is not the standard error shape still gives a usable message")
        void handlesUnexpectedErrorBody() {
            server.expect(anything())
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.TEXT_HTML)
                            .body("<html>Whitelabel Error Page</html>"));

            assertThatThrownBy(() -> client.portfolios())
                    .isInstanceOf(BackendException.class)
                    .hasMessageContaining("500");
        }

        @Test
        @DisplayName("a conflict surfaces the backend's explanation")
        void translatesConflict() {
            server.expect(anything()).andRespond(withStatus(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"status":409,"error":"Conflict","message":"Asset 'x' is already tracked",
                             "fieldErrors":{},"timestamp":"2026-07-31T11:00:00"}
                            """));

            assertThatThrownBy(() -> client.createAsset(new AssetRequest("x", "X", "X")))
                    .isInstanceOf(BackendException.class)
                    .hasMessage("Asset 'x' is already tracked");
        }

        @Test
        @DisplayName("an unreachable backend gets its own message rather than a status code")
        void translatesConnectionFailure() {
            // No stub registered and the host does not resolve, so the request
            // fails at the transport layer rather than returning a status.
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("http://localhost:1/v1");
            PortfolioApiClient offline = new PortfolioApiClient(
                    builder.build(), new ObjectMapper(), 1L);

            assertThatThrownBy(offline::portfolios)
                    .isInstanceOf(BackendException.class)
                    .hasMessageContaining("Cannot reach the backend");
        }
    }
}
