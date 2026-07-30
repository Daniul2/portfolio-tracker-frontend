package com.kodilla.portfolio.ui.view;

import com.kodilla.portfolio.ui.client.BackendDtos.*;
import com.kodilla.portfolio.ui.client.PortfolioApiClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.kodilla.portfolio.ui.view.ViewSupport.*;

/** The transaction ledger: add, edit and delete buys and sells. */
@Route(value = "transactions", layout = MainLayout.class)
@PageTitle("Transactions | Portfolio Tracker")
public class TransactionsView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PortfolioApiClient client;

    private final ComboBox<Portfolio> portfolioPicker = new ComboBox<>("Portfolio");
    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);

    public TransactionsView(PortfolioApiClient client) {
        this.client = client;
        setSizeFull();

        portfolioPicker.setItemLabelGenerator(Portfolio::name);
        portfolioPicker.addValueChangeListener(event -> reload());

        configureGrid();

        Button add = new Button("Add transaction", event -> openEditor(null));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(portfolioPicker, add);
        toolbar.setAlignItems(Alignment.END);

        add(toolbar, grid);
        loadPortfolios();
    }

    private void configureGrid() {
        grid.addColumn(transaction -> transaction.executedAt() == null
                        ? "" : transaction.executedAt().format(TIMESTAMP))
                .setHeader("Date").setAutoWidth(true).setSortable(true);
        grid.addColumn(Transaction::type).setHeader("Type").setAutoWidth(true);
        grid.addColumn(Transaction::symbol).setHeader("Asset").setAutoWidth(true);
        grid.addColumn(transaction -> quantity(transaction.quantity()))
                .setHeader("Quantity").setAutoWidth(true);
        grid.addColumn(transaction -> money(transaction.pricePerUnitUsd(), "USD"))
                .setHeader("Unit price").setAutoWidth(true);
        grid.addColumn(transaction -> money(transaction.feeUsd(), "USD"))
                .setHeader("Fee").setAutoWidth(true);
        grid.addColumn(transaction -> money(transaction.grossValueUsd(), "USD"))
                .setHeader("Total").setAutoWidth(true);
        grid.addColumn(Transaction::note).setHeader("Note").setFlexGrow(1);
        grid.addComponentColumn(this::rowActions).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
    }

    private HorizontalLayout rowActions(Transaction transaction) {
        Button edit = new Button("Edit", event -> openEditor(transaction));
        Button delete = new Button("Delete", event -> {
            if (guard(() -> client.deleteTransaction(transaction.id()))) {
                success("Transaction deleted");
                reload();
            }
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
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
                : guardValue(() -> client.transactions(selected.id()), List.of()));
    }

    /** @param existing null to create a new transaction */
    private void openEditor(Transaction existing) {
        Portfolio portfolio = portfolioPicker.getValue();
        if (portfolio == null) {
            error("Select a portfolio first");
            return;
        }

        Dialog dialog = new Dialog(existing == null ? "New transaction" : "Edit transaction");

        ComboBox<Asset> asset = new ComboBox<>("Asset");
        List<Asset> assets = guardValue(client::assets, List.of());
        asset.setItems(assets);
        asset.setItemLabelGenerator(item -> item.symbol() + " - " + item.name());

        ComboBox<String> type = new ComboBox<>("Type", "BUY", "SELL");
        BigDecimalField quantity = new BigDecimalField("Quantity");
        BigDecimalField price = new BigDecimalField("Price per unit (USD)");
        BigDecimalField fee = new BigDecimalField("Fee (USD)");
        DateTimePicker executedAt = new DateTimePicker("Executed at");
        TextField note = new TextField("Note");

        if (existing == null) {
            type.setValue("BUY");
            fee.setValue(BigDecimal.ZERO);
            executedAt.setValue(LocalDateTime.now());
        } else {
            assets.stream()
                    .filter(item -> item.id().equals(existing.assetId()))
                    .findFirst().ifPresent(asset::setValue);
            type.setValue(existing.type());
            quantity.setValue(existing.quantity());
            price.setValue(existing.pricePerUnitUsd());
            fee.setValue(existing.feeUsd());
            executedAt.setValue(existing.executedAt());
            note.setValue(existing.note() == null ? "" : existing.note());
        }

        FormLayout form = new FormLayout(asset, type, quantity, price, fee, executedAt, note);

        Button save = new Button("Save", event -> {
            if (asset.isEmpty() || type.isEmpty() || quantity.isEmpty() || price.isEmpty()) {
                error("Asset, type, quantity and price are required");
                return;
            }
            TransactionRequest request = new TransactionRequest(
                    portfolio.id(), asset.getValue().id(), type.getValue(),
                    quantity.getValue(), price.getValue(),
                    fee.isEmpty() ? BigDecimal.ZERO : fee.getValue(),
                    executedAt.getValue(), note.getValue());

            boolean ok = existing == null
                    ? guard(() -> client.createTransaction(request))
                    : guard(() -> client.updateTransaction(existing.id(), request));

            if (ok) {
                success(existing == null ? "Transaction added" : "Transaction updated");
                dialog.close();
                reload();
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), save);
        dialog.open();
    }
}
