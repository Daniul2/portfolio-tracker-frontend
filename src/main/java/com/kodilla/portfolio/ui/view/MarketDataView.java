package com.kodilla.portfolio.ui.view;

import com.kodilla.portfolio.ui.client.BackendDtos.ExchangeRate;
import com.kodilla.portfolio.ui.client.BackendDtos.MarketRefreshResult;
import com.kodilla.portfolio.ui.client.BackendDtos.PriceSnapshot;
import com.kodilla.portfolio.ui.client.PortfolioApiClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.kodilla.portfolio.ui.view.ViewSupport.*;

/**
 * Shows what came from each external source, which makes the two-provider
 * design visible rather than hidden behind the dashboard's totals.
 */
@Route(value = "market-data", layout = MainLayout.class)
@PageTitle("Market data | Portfolio Tracker")
public class MarketDataView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PortfolioApiClient client;
    private final Grid<PriceSnapshot> pricesGrid = new Grid<>(PriceSnapshot.class, false);
    private final Grid<ExchangeRate> ratesGrid = new Grid<>(ExchangeRate.class, false);
    private final Span conversionResult = new Span();

    public MarketDataView(PortfolioApiClient client) {
        this.client = client;
        setSizeFull();

        configurePricesGrid();
        configureRatesGrid();

        Button refresh = new Button("Refresh from CoinGecko and NBP", event -> refresh());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new HorizontalLayout(refresh),
                sectionTitle("Latest prices (CoinGecko)"), pricesGrid,
                sectionTitle("Exchange rates (NBP)"), ratesGrid,
                sectionTitle("Convert"), converter());

        reload();
    }

    private H2 sectionTitle(String text) {
        H2 title = new H2(text);
        title.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.Bottom.NONE);
        return title;
    }

    private void configurePricesGrid() {
        pricesGrid.addColumn(PriceSnapshot::symbol).setHeader("Symbol").setAutoWidth(true);
        pricesGrid.addColumn(snapshot -> money(snapshot.priceUsd(), "USD"))
                .setHeader("Price").setAutoWidth(true);
        pricesGrid.addColumn(snapshot -> percent(snapshot.change24hPercent()))
                .setHeader("24h change").setAutoWidth(true);
        pricesGrid.addColumn(snapshot -> snapshot.capturedAt() == null
                        ? "" : snapshot.capturedAt().format(TIMESTAMP))
                .setHeader("Captured").setAutoWidth(true);
        pricesGrid.setHeight("260px");
    }

    private void configureRatesGrid() {
        ratesGrid.addColumn(ExchangeRate::currencyCode).setHeader("Currency").setAutoWidth(true);
        ratesGrid.addColumn(rate -> quantity(rate.ratePln()) + " PLN")
                .setHeader("Value of 1 unit").setAutoWidth(true);
        ratesGrid.addColumn(rate -> rate.effectiveDate() == null ? "" : rate.effectiveDate().toString())
                .setHeader("Effective date").setAutoWidth(true);
        ratesGrid.setHeight("260px");
    }

    private HorizontalLayout converter() {
        TextField currency = new TextField("Convert 1 USD to");
        currency.setPlaceholder("PLN");
        currency.setValue("PLN");

        Button convert = new Button("Convert", event -> {
            String target = currency.getValue() == null ? "" : currency.getValue().trim();
            if (target.isEmpty()) {
                error("Enter a currency code");
                return;
            }
            BigDecimal rate = guardValue(() -> client.usdRate(target), null);
            conversionResult.setText(rate == null ? ""
                    : "1 USD = " + rate.stripTrailingZeros().toPlainString()
                    + " " + target.toUpperCase());
        });

        HorizontalLayout row = new HorizontalLayout(currency, convert, conversionResult);
        row.setAlignItems(Alignment.END);
        return row;
    }

    private void reload() {
        pricesGrid.setItems(guardValue(client::latestPrices, List.of()));
        ratesGrid.setItems(guardValue(client::rates, List.of()));
    }

    private void refresh() {
        MarketRefreshResult result = guardValue(client::refreshMarketData, null);
        if (result == null) {
            return;
        }
        if (result.failures() == null || result.failures().isEmpty()) {
            success("Stored %d price(s) and %d rate(s)"
                    .formatted(result.pricesSaved(), result.ratesSaved()));
        } else {
            // One provider failing must not read as total failure.
            error("Partly refreshed: " + String.join("; ", result.failures()));
        }
        reload();
    }
}
