package com.kodilla.portfolio.ui.view;

import com.kodilla.portfolio.ui.client.BackendException;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;

/** Shared helpers: consistent notifications and number formatting. */
final class ViewSupport {

    private ViewSupport() {
    }

    static void success(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    static void error(String message) {
        Notification notification = Notification.show(message, 6000, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /**
     * Runs a backend call, turning any failure into an error notification rather than an exception
     * that would blank the view.
     */
    static boolean guard(Runnable action) {
        try {
            action.run();
            return true;
        } catch (BackendException e) {
            error(e.getMessage());
            return false;
        }
    }

    /** Same as {@link #guard}, for calls that produce a value. */
    static <T> T guardValue(Supplier<T> action, T fallback) {
        try {
            return action.get();
        } catch (BackendException e) {
            error(e.getMessage());
            return fallback;
        }
    }

    static String money(BigDecimal value, String suffix) {
        if (value == null) {
            return "-";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + suffix;
    }

    static String percent(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        String sign = value.signum() > 0 ? "+" : "";
        return sign + value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    static String quantity(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
