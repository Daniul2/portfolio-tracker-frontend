package com.kodilla.portfolio.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vaadin frontend for the portfolio tracker. Holds no business logic and no
 * database: everything it shows comes from the backend's REST API.
 */
@SpringBootApplication
@Theme("portfolio")
public class FrontendApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
