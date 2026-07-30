# Portfolio Tracker — Frontend (Vaadin)

The user interface for the portfolio tracker, built with [Vaadin](https://vaadin.com/).

It holds no business logic and no database of its own — every screen is filled by
calling the backend's REST API.

**Backend repository:** [portfolio-tracker-backend](../portfolio-tracker-backend)
— the REST API this application consumes. **It must be running**, or the screens
will show a "cannot reach the backend" notification.

---

## Screens

| Screen | What it does |
|---|---|
| **Dashboard** | Portfolio value in USD and in its own currency, profit/loss per holding, recent alerts |
| **Transactions** | Add, edit and delete buys and sells |
| **Alerts** | Create price and portfolio-value thresholds, pause them, check them on demand |
| **Assets** | Manage which assets are tracked and which the scheduler prices |
| **Market data** | Raw CoinGecko prices and NBP rates, plus a currency converter |

The Market data screen exists mainly to make the two data sources visible rather
than hidden behind the dashboard's totals.

---

## Running it

### Requirements

- **JDK 17 or newer**
- The backend running (see below)
- An internet connection on the first build only — Vaadin downloads a Node
  toolchain and npm packages, which takes several minutes. Later builds are fast.

### Start the backend first

In the backend repository:

```bash
./gradlew bootRun
```

### Then start this application

```bash
./gradlew bootRun
```

Open **http://localhost:8081**.

### If the backend is on a different port

Port 8080 is often already taken (Apache, XAMPP and Jenkins all default to it).
If you started the backend elsewhere, point this application at it:

```bash
./gradlew bootRun --args='--app.backend.base-url=http://localhost:8090/v1'
```

Two things must agree:

1. this application's `app.backend.base-url` must match where the backend is;
2. the backend's `app.cors.allowed-origins` must include this application's
   address (`http://localhost:8081` by default).

---

## Configuration

In `src/main/resources/application.properties`, overridable with `--property=value`:

| Property | Default | Purpose |
|---|---|---|
| `server.port` | `8081` | Port this UI runs on |
| `app.backend.base-url` | `http://localhost:8080/v1` | Where the REST API is |
| `app.backend.timeout-seconds` | `20` | Per-request timeout |
| `app.backend.user-id` | `1` | Whose data to show — the seeded demo user |

There is no login. The course project does not require authentication, so the UI
shows one user's data, chosen by `app.backend.user-id`. The backend seeds that
user on first start.

---

## How it is put together

```
com.kodilla.portfolio.ui
├── FrontendApplication.java     Spring Boot entry point
├── client/
│   ├── BackendDtos.java         Mirrors of the API's JSON shapes
│   ├── BackendException.java    A failure with a message fit to show a user
│   └── PortfolioApiClient.java  The only route to the backend
└── view/
    ├── MainLayout.java          Shell and navigation
    ├── ViewSupport.java         Shared notifications and formatting
    ├── DashboardView.java
    ├── TransactionsView.java
    ├── AlertsView.java
    ├── AssetsView.java
    └── MarketDataView.java
```

Two decisions worth explaining:

**The DTOs are duplicated, deliberately.** They mirror the backend's response
records but are declared separately here. The two applications share no code,
only the REST contract, which is what the course brief asks for. They ignore
unknown JSON fields, so the backend can add a field without breaking this client.

**All errors become notifications, never stack traces.** `PortfolioApiClient`
translates every failure into a `BackendException` carrying a message already fit
to display — pulled from the API's error body when there is one. `ViewSupport.guard`
wraps each call so a failure shows a red notification instead of blanking the view.
A validation error from the backend arrives as a readable field-by-field message.

### Build

```bash
./gradlew build
```

Production build (bundles and minifies the frontend):

```bash
./gradlew build -Pvaadin.productionMode=true
```

---

## Why these versions

**Vaadin is pinned to 24.7.4, and this project uses Gradle 8** while the backend
uses Gradle 9. That combination is deliberate, and changing either half breaks
the build in a way that is not obvious from the error message:

- **Vaadin 24.8 and newer require a Vaadin account.** Their `VaadinServlet`
  calls a license checker on startup and throws `LicenseException` if it cannot
  validate, so the application will not start at all. Anyone cloning this
  repository would have to sign up before they could run it.
- **The Vaadin 24.7 Gradle plugin does not work on Gradle 9.** It calls
  `ResolvedConfiguration.getFiles()`, which Gradle 9 removed, and the build fails
  during `vaadinPrepareFrontend`.

Vaadin 24.7.4 on Gradle 8.14.3 avoids both: no account needed, and the build
works. The backend is unaffected and stays on Gradle 9.
