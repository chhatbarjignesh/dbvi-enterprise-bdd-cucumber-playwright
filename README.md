# Dbvi Enterprise Playwright Cucumber BDD Framework

[![Framework Version](https://img.shields.io/badge/Playwright-1.52.0-blue.svg)](https://playwright.dev/java/)
[![Java Version](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://openjdk.org/)
[![Build Status](https://img.shields.io/badge/Build-Success-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/Platform-Darwin%20%7C%20Linux%20%7C%20Windows-lightgrey.svg)]()

Welcome to the **Dbvi Enterprise Playwright Cucumber BDD Automation Framework**. This repository hosts a lightweight, highly decoupled, thread-safe, and high-performance automation suite optimized for parallel execution, enterprise integrations, and rock-solid execution stability on slow/latent staging platforms.

> 📖 For a full architectural breakdown (layer diagrams, data flow, and component responsibilities), see **[FRAMEWORK_MAP.md](FRAMEWORK_MAP.md)**.
> 🤖 For AI-assistant conventions when extending this codebase, see **[CLAUDE.md](CLAUDE.md)** (Claude Code).

---

## 🚀 Key Framework Capabilities (Architect's Spotlight)

1. **Pure Playwright Execution Engine**: Completely purged of legacy Selenium or Appium dependencies, running 100% on Playwright's native WebSocket protocol (`1.52.0`) for blisteringly fast command dispatches.
2. **Dynamic PostgreSQL DB Integration**:
   * **Active SKU Retrievals**: Dynamically queries the `"EXECUTION".sku_details` table over JDBC using random single-row retrievals matching your active staging profile (e.g., `QA1`).
   * **Thread-Safe User Account Locking**: Queries `"EXECUTION".userdetails` to fetch an unlocked user account matching your Gherkin type (e.g., `regularUser`), dynamically locks the user via `lockedstatus = 'Y'` to prevent parallel conflicts, and automatically **unlocks** the account inside the database upon test teardown (`@After`).
3. **Dynamic Multi-Project Modular Isolation**:
   * Supports unlimited separate business applications (e.g., `dsp`, `dmob`, `gmob`) under isolated subdirectories for features, step-definitions, and environment YAML files.
   * Completely avoids asset pollution and naming collisions across different teams.
4. **Secure Encryption Engine**: Fully integrates the corporate `PBEWithMD5AndDES` password-based encryption standard, allowing database passwords to be stored as encrypted strings and decrypted strictly in memory on-the-fly. *(Note: the PBE key/salt ship inside this repo, so this scheme only obscures the password from casual viewing — see the [Framework Map](FRAMEWORK_MAP.md#security-notes) for guidance on rotating to a real secrets manager.)*
5. **Zero-Flakiness Dynamic `WebAction` Wrapper**:
   * Completely eliminates brittle static sleeps and `Thread.sleep` calls.
   * Automatically enforces dynamic pre-interaction wait-for-visibility states on every element before executing clicks or text inputs.
   * Features a native **JavaScript Click Fallback** (`clickUsingJS`) to seamlessly bypass sticky/fixed headers, responsive animations, or unscrollable viewport conflicts on remote VM environments.
6. **Headed Local, Remote Grid, and Perfecto Cloud Support**:
   * Natively connects to **Selenium Grid 4** via `SELENIUM_REMOTE_URL` WebSocket dispatches.
   * Establishes handshakes with the **Perfecto Cloud**, automatically calculating browser VM dimensions, and querying Perfecto’s Public Export API post-execution to map correct Smart Report video links directly.
7. **Assertion-Free Step Definitions**:
   * Enforces strict Single-Class Page Object Models (POM).
   * All validations and AssertJ assertions reside exclusively inside Page Object verification methods to maintain high cohesion and follow Playwright core recommendations.
8. **Programmatic Allure Metadata & Failure Classification**:
   * Dynamically compiles and writes `environment.properties` (including active project module, URL, resolution, browser, and CI-passed parameters) directly to results on suite completion.
   * Leverages a customized, regex-matched `categories.json` template to dynamically group failures into clear operational buckets (Product Defects, Locators/Timeouts, Database, Infrastructure) inside Allure.
9. **REST-Driven ReportPortal Email Notifier**:
   * Connects directly to ReportPortal's REST API using REST Assured, programmatically extracting execution metrics and failure stack traces.
   * Automatically compiles and dispatches a rich, custom-styled HTML report via SMTP (`ReportPortalEmailNotifier.java`) on every execution.

---

## 📂 Codebase Directory Mapping

```text
dbvi-enterprise-bdd-cucumber-playwright/
├── pom.xml                                       # Standardized dependency versions (Playwright 1.52.0, AspectJ 1.9.25.1)
├── testng.xml                                    # Central TestNG execution suite mapping with parallel thread constraints
├── README.md                                     # Enterprise framework documentation and guides
├── src/
│   ├── main/java/com/dbvi/automation/
│   │     ├── framework/
│   │     │     ├── config/
│   │     │     │     └── FrameworkProperties.java # Property loader supporting override flags
│   │     │     ├── factory/
│   │     │     │     ├── DriverFactory.java       # Thread-local storage for Playwright context
│   │     │     │     └── DriverHelper.java        # DRY helper for local, grid, and cloud startups
│   │     │     ├── loggers/
│   │     │     │     └── ReportLogger.java        # Unified logging facade mapped to ReportPortal
│   │     │     ├── utils/
│   │     │     │     ├── EncryptionUtil.java      # PBE DB password encrypter/decrypter
│   │     │     │     ├── TestData.java            # Dot-notation-aware YAML config parser
│   │     │     │     ├── TotpUtil.java            # Google Authenticator 2FA TOTP generator
│   │     │     │     ├── AllureReportHelper.java  # Auto-compiler for Allure properties & categories
│   │     │     │     ├── ReportPortalEmailNotifier.java # REST-driven SMTP HTML email report dispatcher
│   │     │     │     └── testdata/
│   │     │     │           └── UserCredentialsProvider.java # DB-driven user credentials loader + lock manager
│   │     │     └── wrapper/ui/
│   │     │           └── WebAction.java           # Fluent wait-to-click Playwright wrappers
│   │     └── pages/
│   │           ├── BasePage.java                  # Shared POM base resolving active Page context
│   │           └── example/
│   │                 └── HomePage.java            # Minimal reference Page Object (navigate + getBodyText)
│   └── test/
│       ├── java/com/dbvi/automation/
│       │     ├── runner/
│       │     │     ├── RunCucumberTest.java       # Primary TestNG parallel BDD runner with Allure hooks
│       │     │     ├── ReRunCucumberTest.java     # Automated retry executor for failed runs (merges to ReportPortal)
│       │     │     └── DynamicSuiteListener.java  # IAlterSuiteListener wiring the rerun stage into testng.xml
│       │     └── steps/
│       │           ├── ExampleSteps.java          # Generic steps backing test.feature
│       │           └── Hooks.java                 # @Before/@After/@AfterStep lifecycle (driver, screenshots, Perfecto)
│       └── resources/
│           ├── config.properties                  # Global execution switches & SSO/DB keys
│           ├── reportportal.properties            # Core ReportPortal credentials and endpoint mappings
│           ├── allure.properties                  # Unifies Cucumber results path under target/allure-results
│           ├── allure/
│           │     └── categories.json              # Custom failure categorizations template for Allure
│           ├── features/
│           │     └── test.feature                 # Working example scenarios (generic, no client-specific content)
│           └── env/
│                 └── example/                     # Test-data backing test.feature
│                       ├── QA/
│                       ├── QA1/
│                       └── UAT/
```

> ℹ️ This repo ships with only the generic `example` project module (`pages/example/HomePage.java` + `steps/ExampleSteps.java` + `test.feature` + `env/example/`), wired up and working out of the box. The prior `dsp`/`events` business-specific modules were removed as client-specific — add your own project module following the same four-location convention (see [FRAMEWORK_MAP.md](FRAMEWORK_MAP.md) § Extending the Framework).

---

## 🛠️ Configuration Settings (`src/test/resources/config.properties`)

Configure your target environments, parallel scopes, cloud integrations, and credentials inside the main properties file:

```properties
# Primary Execution Engine
browserName=chromium
browserTest=true
headless=false
project-name=example
env=QA1
url=https://testmms.na.dadabhagwan.org/
playwright.timeout=60
mweb=false

# Interactive Screenshots & Vision Options
screenshot.afterStep=false
screenshot.fullPage=true

# Remote Selenium Grid 4 Settings
grid.enabled=false
grid.url=

# Perfecto Cloud Run Settings
perfecto.enabled=false
perfecto.url=wss://dbvi.perfectomobile.com/websocket
perfecto.token=YOUR_PERFECTO_TOKEN
perfecto.platformName=Windows
perfecto.platformVersion=11
perfecto.browserName=Chrome
perfecto.browserVersion=latest
perfecto.jobName=Playwright BDD Job
perfecto.projectName=example
perfecto.jobNumber=1
resolution=1920x1080
perfecto.location=US East

# Corporate Database Configurations (PBE-Encrypted db.password)
db.url=
db.username=testautomation
db.password=YOUR_ENCRYPTED_DB_PASSWORD
read-credentials-from-db=false

# SSO Credentials & Google Authenticator TOTP configurations
sso.username=your_sso_username@dbvi.com
sso.password=your_sso_password
sso.totpSecret=BP26 TDZU Z5SV PZJR

# Email Notification Configurations
email.recipients=
email.cc=cc_email@dbvi.com
```

---

## 💻 Command Line Execution Guide

This framework integrates with **AspectJ compilation weaving** to log and intercept steps natively. Always run tests using the Maven compiler plugin lifecycle:

### 1. Compile the framework
```bash
mvn clean test-compile
```

### 2. Run all features locally on standard desktop Chrome
```bash
mvn clean test
```

### 3. Run a specific tag expression (e.g., the built-in smoke example via Cucumber 7 Tags)
```bash
mvn clean test -Dcucumber.filter.tags="@SMOKE"
```

### 4. Enable database-driven user credentials loading and dynamic locking
```bash
mvn clean test -Dread-credentials-from-db=true
```

### 5. Control parallel thread pool concurrency dynamically (Defaults to 4)
```bash
mvn clean test -Dthread.count=8
```

### 6. Run headless inside remote Selenium Grid 4 nodes
```bash
mvn clean test -Dgrid.enabled=true -Dheadless=true
```

### 7. Run fully parallel inside the Perfecto Cloud Browser VMs
```bash
mvn clean test -Dperfecto.enabled=true -Dheadless=true
```

### 8. Automatically format and indent all Java source files
```bash
mvn spotless:apply
```
This formats all Java files inside your repository according to the Google Java Format standard (AOSP 4-space indentation), trims trailing whitespaces, and removes unused imports instantly!

---

## 📈 Rerun Capabilities (Auto-Healing Pipelines)

To bulletproof your CI/CD pipelines and save execution time, this framework implements a **consolidated single-suite auto-healing rerun pipeline**:
1. **Programmatic In-Memory Suite Interception**: When Maven starts the TestNG suite, our custom **`DynamicSuiteListener.java`** (a native TestNG `IAlterSuiteListener`) intercepts the suite at startup.
2. **Dynamic Suite Assembly**: 
   * If `rerun.enabled` is `true` in `config.properties`, the listener dynamically appends **`ReRunCucumberTest`** sequentially right after your primary `RunCucumberTest` inside the TestNG suite in-memory.
   * If `rerun.enabled` is `false`, the listener completely excludes the rerun class from the active suite.
3. **Automated File Pre-Creation**: To satisfy Cucumber 7's strict classloading requirements, `DynamicSuiteListener` programmatically pre-creates an empty `rerun.txt` file on disk before class initialization, preventing any `NoSuchFileException` crashes on 100% passed runs.
4. **Execution Flow**:
   * `RunCucumberTest` executes all standard scenarios in parallel. Any scenario that fails writes its failure details straight to `target/cucumber-reports/rerun-reports/rerun.txt`.
   * `ReRunCucumberTest` immediately and sequentially executes, reading `rerun.txt` and retrying **strictly the failed scenarios** inside the same JVM process!
5. **Real-Time ReportPortal Merging**: Because both runs execute under a **single TestNG suite session**, the primary ReportPortal launch remains open, allowing all retry statistics and logs to **merge dynamically in real-time**! The retried scenario shows up inside Allure as a single entry with a "Retries" history tab, keeping your dashboards beautifully clean.

---

## 🤖 Claude Code Skills

This repository ships a workspace-scoped Claude Code skill under `.claude/skills/` to speed up common AI-assisted workflows:

*   **`dbvi-playwright-bdd`**: Step-by-step guidance for authoring new BDD features, single-class Page Objects, and step definitions that follow this repo's conventions (see [CLAUDE.md](CLAUDE.md)).

Invoke it explicitly in a Claude Code session with `/dbvi-playwright-bdd`.

---

*Designed and engineered with absolute architectural precision for DBVI.*
