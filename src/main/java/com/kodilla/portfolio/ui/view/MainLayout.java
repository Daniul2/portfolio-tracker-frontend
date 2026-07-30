package com.kodilla.portfolio.ui.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

/** Shell with the navigation drawer shared by every view. */
public class MainLayout extends AppLayout {

    public MainLayout() {
        addToNavbar(new DrawerToggle(), title());
        addToDrawer(navigation());
        setPrimarySection(Section.DRAWER);
    }

    private H1 title() {
        H1 title = new H1("Portfolio Tracker");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);
        return title;
    }

    private VerticalLayout navigation() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", DashboardView.class));
        nav.addItem(new SideNavItem("Transactions", TransactionsView.class));
        nav.addItem(new SideNavItem("Alerts", AlertsView.class));
        nav.addItem(new SideNavItem("Assets", AssetsView.class));
        nav.addItem(new SideNavItem("Market data", MarketDataView.class));

        Span hint = new Span("Data from CoinGecko and NBP");
        hint.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY,
                LumoUtility.Margin.MEDIUM);

        VerticalLayout drawer = new VerticalLayout(nav, hint);
        drawer.setPadding(false);
        drawer.setSpacing(false);
        return drawer;
    }
}
