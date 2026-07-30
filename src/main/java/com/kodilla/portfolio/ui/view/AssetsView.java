package com.kodilla.portfolio.ui.view;

import com.kodilla.portfolio.ui.client.BackendDtos.Asset;
import com.kodilla.portfolio.ui.client.BackendDtos.AssetRequest;
import com.kodilla.portfolio.ui.client.PortfolioApiClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

import static com.kodilla.portfolio.ui.view.ViewSupport.*;

/**
 * The catalogue of tracked assets. Only active assets are priced by the
 * backend's scheduler, so pausing one stops it consuming API quota.
 */
@Route(value = "assets", layout = MainLayout.class)
@PageTitle("Assets | Portfolio Tracker")
public class AssetsView extends VerticalLayout {

    private final PortfolioApiClient client;
    private final Grid<Asset> grid = new Grid<>(Asset.class, false);

    public AssetsView(PortfolioApiClient client) {
        this.client = client;
        setSizeFull();

        configureGrid();

        Button add = new Button("Track new asset", event -> openEditor());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Span hint = new Span("The identifier must match CoinGecko's, for example "
                + "\"bitcoin\" or \"ethereum\".");
        hint.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        add(new HorizontalLayout(add), hint, grid);
        reload();
    }

    private void configureGrid() {
        grid.addColumn(Asset::symbol).setHeader("Symbol").setAutoWidth(true).setSortable(true);
        grid.addColumn(Asset::name).setHeader("Name").setFlexGrow(1);
        grid.addColumn(Asset::externalId).setHeader("CoinGecko id").setAutoWidth(true);
        grid.addComponentColumn(this::statusBadge).setHeader("Priced by scheduler").setAutoWidth(true);
        grid.addComponentColumn(this::rowActions).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
    }

    private Span statusBadge(Asset asset) {
        Span badge = new Span(asset.active() ? "yes" : "paused");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(asset.active() ? "success" : "contrast");
        return badge;
    }

    private HorizontalLayout rowActions(Asset asset) {
        Button toggle = new Button(asset.active() ? "Pause" : "Resume", event -> {
            if (guard(() -> client.setAssetActive(asset.id(), !asset.active()))) {
                success(asset.active() ? "Price tracking paused" : "Price tracking resumed");
                reload();
            }
        });

        Button delete = new Button("Delete", event -> {
            // The backend refuses if transactions reference the asset, and that
            // message is surfaced as-is.
            if (guard(() -> client.deleteAsset(asset.id()))) {
                success("Asset deleted");
                reload();
            }
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        return new HorizontalLayout(toggle, delete);
    }

    private void reload() {
        grid.setItems(guardValue(client::assets, List.of()));
    }

    private void openEditor() {
        Dialog dialog = new Dialog("Track a new asset");

        TextField externalId = new TextField("CoinGecko id");
        externalId.setPlaceholder("bitcoin");
        TextField symbol = new TextField("Symbol");
        symbol.setPlaceholder("BTC");
        TextField name = new TextField("Name");
        name.setPlaceholder("Bitcoin");

        Button save = new Button("Save", event -> {
            if (externalId.isEmpty() || symbol.isEmpty() || name.isEmpty()) {
                error("All three fields are required");
                return;
            }
            AssetRequest request = new AssetRequest(
                    externalId.getValue().trim(), symbol.getValue().trim(), name.getValue().trim());
            if (guard(() -> client.createAsset(request))) {
                success("Asset added");
                dialog.close();
                reload();
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new FormLayout(externalId, symbol, name));
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), save);
        dialog.open();
    }
}
