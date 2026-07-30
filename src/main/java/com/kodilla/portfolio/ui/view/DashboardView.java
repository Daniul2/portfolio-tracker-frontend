package com.kodilla.portfolio.ui.view;

import com.kodilla.portfolio.ui.client.BackendDtos.*;
import com.kodilla.portfolio.ui.client.PortfolioApiClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.kodilla.portfolio.ui.view.ViewSupport.*;

/**
 * Main screen: pick a portfolio, see what it is worth now in USD and in the
 * portfolio's own currency, and see the profit or loss per position.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | Portfolio Tracker")
public class DashboardView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PortfolioApiClient client;

    private final ComboBox<Portfolio> portfolioPicker = new ComboBox<>("Portfolio");
    private final Grid<Holding> holdingsGrid = new Grid<>(Holding.class, false);
    private final HorizontalLayout statsRow = new HorizontalLayout();
    private final Span valuedAt = new Span();
    private final Grid<AlertEvent> eventsGrid = new Grid<>(AlertEvent.class, false);

    public DashboardView(PortfolioApiClient client) {
        this.client = client;
        setSizeFull();
        setPadding(true);

        configurePicker();
        configureHoldingsGrid();
        configureEventsGrid();

        statsRow.setWidthFull();
        // Let the stat cards wrap onto a second row on a narrow window.
        statsRow.getStyle().set("flex-wrap", "wrap");
        valuedAt.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

        add(toolbar(), statsRow, valuedAt,
                sectionTitle("Holdings"), holdingsGrid,
                sectionTitle("Recent alerts"), eventsGrid);

        loadPortfolios();
    }

    private H2 sectionTitle(String text) {
        H2 title = new H2(text);
        title.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.NONE);
        return title;
    }

    private HorizontalLayout toolbar() {
        Button refresh = new Button("Refresh market data", event -> refreshMarketData());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button reload = new Button("Reload", event -> reloadSelected());

        HorizontalLayout toolbar = new HorizontalLayout(portfolioPicker, refresh, reload);
        toolbar.setAlignItems(Alignment.END);
        return toolbar;
    }

    private void configurePicker() {
        portfolioPicker.setItemLabelGenerator(Portfolio::name);
        portfolioPicker.addValueChangeListener(event -> reloadSelected());
    }

    private void configureHoldingsGrid() {
        holdingsGrid.addColumn(Holding::symbol).setHeader("Symbol").setAutoWidth(true);
        holdingsGrid.addColumn(Holding::assetName).setHeader("Asset").setAutoWidth(true);
        holdingsGrid.addColumn(holding -> quantity(holding.quantity()))
                .setHeader("Quantity").setAutoWidth(true);
        holdingsGrid.addColumn(holding -> money(holding.averageCostUsd(), "USD"))
                .setHeader("Avg. cost").setAutoWidth(true);
        holdingsGrid.addColumn(holding -> holding.priced()
                        ? money(holding.currentPriceUsd(), "USD") : "not priced yet")
                .setHeader("Price now").setAutoWidth(true);
        holdingsGrid.addColumn(holding -> money(holding.marketValueUsd(), "USD"))
                .setHeader("Value").setAutoWidth(true);
        holdingsGrid.addComponentColumn(this::profitBadge).setHeader("Profit / loss").setAutoWidth(true);
        holdingsGrid.addColumn(holding -> money(holding.marketValueBase(), ""))
                .setHeader("Value (base currency)").setAutoWidth(true);
        holdingsGrid.setHeight("340px");
    }

    /** Green for a gain, red for a loss, so the grid is scannable at a glance. */
    private Span profitBadge(Holding holding) {
        if (!holding.priced() || holding.unrealizedPnlUsd() == null) {
            return new Span("-");
        }
        Span badge = new Span(money(holding.unrealizedPnlUsd(), "USD")
                + " (" + percent(holding.unrealizedPnlPercent()) + ")");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList()
                .add(holding.unrealizedPnlUsd().signum() >= 0 ? "success" : "error");
        return badge;
    }

    private void configureEventsGrid() {
        eventsGrid.addColumn(event -> event.createdAt() == null
                        ? "" : event.createdAt().format(TIMESTAMP))
                .setHeader("When").setAutoWidth(true);
        eventsGrid.addColumn(AlertEvent::message).setHeader("Message").setFlexGrow(1);
        eventsGrid.addComponentColumn(this::acknowledgeButton).setHeader("").setAutoWidth(true);
        eventsGrid.setHeight("220px");
    }

    private Button acknowledgeButton(AlertEvent event) {
        if (event.acknowledged()) {
            Button seen = new Button("Seen");
            seen.setEnabled(false);
            return seen;
        }
        return new Button("Acknowledge", click -> {
            if (guard(() -> client.acknowledgeEvent(event.id()))) {
                success("Alert acknowledged");
                reloadSelected();
            }
        });
    }

    private void loadPortfolios() {
        List<Portfolio> portfolios = guardValue(client::portfolios, List.of());
        portfolioPicker.setItems(portfolios);
        if (!portfolios.isEmpty()) {
            portfolioPicker.setValue(portfolios.get(0));
        } else {
            statsRow.removeAll();
            statsRow.add(new Span("No portfolios yet. Create one on the Transactions screen."));
        }
    }

    private void reloadSelected() {
        Portfolio selected = portfolioPicker.getValue();
        if (selected == null) {
            holdingsGrid.setItems(List.of());
            eventsGrid.setItems(List.of());
            statsRow.removeAll();
            return;
        }

        PortfolioSummary summary = guardValue(() -> client.summary(selected.id()), null);
        if (summary == null) {
            return;
        }

        holdingsGrid.setItems(summary.holdings() == null ? List.of() : summary.holdings());
        renderStats(summary);
        valuedAt.setText(summary.valuedAt() == null ? ""
                : "Valued at " + summary.valuedAt().format(TIMESTAMP));

        eventsGrid.setItems(guardValue(() -> client.alertEvents(selected.id()), List.of()));
    }

    private void renderStats(PortfolioSummary summary) {
        statsRow.removeAll();
        statsRow.add(
                stat("Total cost", money(summary.totalCostUsd(), "USD")),
                stat("Market value", money(summary.totalValueUsd(), "USD")),
                stat("Profit / loss", money(summary.totalPnlUsd(), "USD")
                        + "  " + percent(summary.totalPnlPercent())),
                stat("Value in " + summary.baseCurrency(),
                        money(summary.totalValueBase(), summary.baseCurrency())),
                stat("USD rate", fxLabel(summary)));
    }

    /** Spells out where the base-currency figure came from, or why it is absent. */
    private String fxLabel(PortfolioSummary summary) {
        BigDecimal rate = summary.fxRate();
        if (rate == null) {
            return "no rate yet - refresh market data";
        }
        return "1 USD = " + rate.stripTrailingZeros().toPlainString() + " " + summary.baseCurrency();
    }

    private VerticalLayout stat(String label, String value) {
        Span caption = new Span(label);
        caption.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
        Span figure = new Span(value);
        figure.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);

        VerticalLayout card = new VerticalLayout(caption, figure);
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidth("240px");
        card.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.MEDIUM);
        return card;
    }

    private void refreshMarketData() {
        MarketRefreshResult result = guardValue(client::refreshMarketData, null);
        if (result == null) {
            return;
        }
        if (result.failures() == null || result.failures().isEmpty()) {
            success("Refreshed %d price(s) and %d rate(s); %d alert(s) triggered"
                    .formatted(result.pricesSaved(), result.ratesSaved(), result.alertsTriggered()));
        } else {
            error("Partly refreshed: " + String.join("; ", result.failures()));
        }
        reloadSelected();
    }
}
