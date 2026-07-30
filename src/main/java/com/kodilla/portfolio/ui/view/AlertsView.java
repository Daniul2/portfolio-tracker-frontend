package com.kodilla.portfolio.ui.view;

import com.kodilla.portfolio.ui.client.BackendDtos.*;
import com.kodilla.portfolio.ui.client.PortfolioApiClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.kodilla.portfolio.ui.view.ViewSupport.*;

/**
 * User-defined price and portfolio-value thresholds. These are notifications
 * the user sets for themselves; the application never suggests trades.
 */
@Route(value = "alerts", layout = MainLayout.class)
@PageTitle("Alerts | Portfolio Tracker")
public class AlertsView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> PRICE_TYPES = List.of("PRICE_ABOVE", "PRICE_BELOW");
    private static final List<String> ALL_TYPES = List.of(
            "PRICE_ABOVE", "PRICE_BELOW", "PORTFOLIO_VALUE_ABOVE", "PORTFOLIO_VALUE_BELOW");

    private final PortfolioApiClient client;

    private final ComboBox<Portfolio> portfolioPicker = new ComboBox<>("Portfolio");
    private final Grid<Alert> grid = new Grid<>(Alert.class, false);

    public AlertsView(PortfolioApiClient client) {
        this.client = client;
        setSizeFull();

        portfolioPicker.setItemLabelGenerator(Portfolio::name);
        portfolioPicker.addValueChangeListener(event -> reload());

        configureGrid();

        Button add = new Button("Add alert", event -> openEditor(null));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button evaluate = new Button("Check alerts now", event -> {
            Integer fired = guardValue(client::evaluateAlerts, null);
            if (fired != null) {
                success(fired + " alert(s) triggered");
                reload();
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(portfolioPicker, add, evaluate);
        toolbar.setAlignItems(Alignment.END);

        add(toolbar, grid);
        loadPortfolios();
    }

    private void configureGrid() {
        grid.addColumn(Alert::type).setHeader("Type").setAutoWidth(true);
        grid.addColumn(alert -> alert.symbol() == null ? "whole portfolio" : alert.symbol())
                .setHeader("Watching").setAutoWidth(true);
        grid.addColumn(alert -> quantity(alert.threshold())).setHeader("Threshold").setAutoWidth(true);
        grid.addComponentColumn(this::statusBadge).setHeader("Status").setAutoWidth(true);
        grid.addColumn(alert -> alert.lastTriggeredAt() == null
                        ? "never" : alert.lastTriggeredAt().format(TIMESTAMP))
                .setHeader("Last triggered").setAutoWidth(true);
        grid.addComponentColumn(this::rowActions).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
    }

    private Span statusBadge(Alert alert) {
        Span badge = new Span(alert.active() ? "active" : "paused");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(alert.active() ? "success" : "contrast");
        return badge;
    }

    private HorizontalLayout rowActions(Alert alert) {
        Button toggle = new Button(alert.active() ? "Pause" : "Resume", event -> {
            AlertRequest request = new AlertRequest(alert.portfolioId(), alert.assetId(),
                    alert.type(), alert.threshold(), !alert.active());
            if (guard(() -> client.updateAlert(alert.id(), request))) {
                success(alert.active() ? "Alert paused" : "Alert resumed");
                reload();
            }
        });

        Button edit = new Button("Edit", event -> openEditor(alert));

        Button delete = new Button("Delete", event -> {
            if (guard(() -> client.deleteAlert(alert.id()))) {
                success("Alert deleted");
                reload();
            }
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        return new HorizontalLayout(toggle, edit, delete);
    }

    private void loadPortfolios() {
        List<Portfolio> portfolios = guardValue(client::portfolios, List.of());
        portfolioPicker.setItems(portfolios);
        if (!portfolios.isEmpty()) {
            portfolioPicker.setValue(portfolios.get(0));
        }
    }

    private void reload() {
        Portfolio selected = portfolioPicker.getValue();
        grid.setItems(selected == null
                ? List.of()
                : guardValue(() -> client.alerts(selected.id()), List.of()));
    }

    private void openEditor(Alert existing) {
        Portfolio portfolio = portfolioPicker.getValue();
        if (portfolio == null) {
            error("Select a portfolio first");
            return;
        }

        Dialog dialog = new Dialog(existing == null ? "New alert" : "Edit alert");

        ComboBox<String> type = new ComboBox<>("Type");
        type.setItems(ALL_TYPES);

        ComboBox<Asset> asset = new ComboBox<>("Asset");
        List<Asset> assets = guardValue(client::assets, List.of());
        asset.setItems(assets);
        asset.setItemLabelGenerator(item -> item.symbol() + " - " + item.name());
        asset.setHelperText("Required for price alerts only");

        BigDecimalField threshold = new BigDecimalField("Threshold");

        // Portfolio-level alerts have no asset, so hide the field for them.
        type.addValueChangeListener(event ->
                asset.setVisible(PRICE_TYPES.contains(event.getValue())));

        if (existing == null) {
            type.setValue("PRICE_ABOVE");
        } else {
            type.setValue(existing.type());
            threshold.setValue(existing.threshold());
            assets.stream()
                    .filter(item -> item.id().equals(existing.assetId()))
                    .findFirst().ifPresent(asset::setValue);
        }
        asset.setVisible(PRICE_TYPES.contains(type.getValue()));

        Button save = new Button("Save", event -> {
            if (type.isEmpty() || threshold.isEmpty()) {
                error("Type and threshold are required");
                return;
            }
            boolean priceAlert = PRICE_TYPES.contains(type.getValue());
            if (priceAlert && asset.isEmpty()) {
                error("Pick an asset for a price alert");
                return;
            }

            AlertRequest request = new AlertRequest(
                    portfolio.id(),
                    priceAlert ? asset.getValue().id() : null,
                    type.getValue(), threshold.getValue(),
                    existing == null ? Boolean.TRUE : existing.active());

            boolean ok = existing == null
                    ? guard(() -> client.createAlert(request))
                    : guard(() -> client.updateAlert(existing.id(), request));

            if (ok) {
                success(existing == null ? "Alert created" : "Alert updated");
                dialog.close();
                reload();
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new FormLayout(type, asset, threshold));
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), save);
        dialog.open();
    }
}
