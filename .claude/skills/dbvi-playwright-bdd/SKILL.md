---
name: dbvi-playwright-bdd
description: Dbvi Playwright Cucumber BDD test case developer. Use when adding, refactoring, or extending BDD features, Gherkin steps, single-class Page Objects, or dynamic database test-data retrieval configurations in this repository.
---

# Dbvi Playwright BDD Developer Skill

This skill provides expert procedural guidance to autonomously develop and extend test suites in the Dbvi Playwright Cucumber BDD framework. It mirrors and operationalizes the conventions already documented in `CLAUDE.md` / `FRAMEWORK_MAP.md` — treat this skill as the step-by-step playbook for actually doing the work described there.

## 📂 Framework Reference Guides

To keep this skill lean, detailed package mappings, properties schemas, and database locking lifecycle guides are decoupled into a dedicated reference sheet. Review it as needed during execution:

*   **API & Directory Standards**: See [references/framework-standards.md](references/framework-standards.md) for full directory mappings, property configurations, and database credentials schema.

---

## 🏛️ Core Architectural Principles

Any test case developed using this skill **must** strictly adhere to these standards:

1.  **Pure Playwright Engine**: Zero Selenium or Appium dependencies in any developed package.
2.  **Single-Class Page Object Model (POM)**: Declare all CSS/XPath selectors (as `private static final String` constants) and all actions/assertions inside a **single Page class**. Never split into separate page/locator/action files.
3.  **Assertion-Free Step Definitions**: Step definitions must remain lightweight, decoupled, and **assertion-free**. All validations (AssertJ) reside exclusively inside Page Object classes.
4.  **Zero-Flakiness WebAction Wrapper**: Never write static sleeps (`Thread.sleep` or raw `page.waitForTimeout`) inside page methods. Always use the inherited `webAction` instance (from `BasePage`), which enforces dynamic pre-interaction wait-for-visibility before every action.
5.  **JavaScript Click Fallbacks**: If a standard click fails due to viewport-clipping or sticky-header overlays, use `webAction.clickUsingJS(selector)` instead of adding a wait/retry loop.
6.  **Playwright MCP Real-Time DOM Inspection**: When Playwright MCP tools are available (`mcp__playwright__browser_navigate`, `mcp__playwright__browser_snapshot`, `mcp__playwright__browser_evaluate`, `mcp__playwright__browser_click`, etc.), use them to inspect the live environment's DOM and accessibility tree before writing selectors. Never guess selectors; verify them against the real page. Prefer stable CSS ID/attribute selectors (e.g. `#email`) or accessible roles over fragile XPaths, and confirm the selector's uniqueness before committing to it.

---

## 🛠️ Step-by-Step Test Case Development Workflow

Follow this four-phase procedure to develop any new BDD test case:

### 📋 Phase 1: Gherkin Feature File Creation
Create a `.feature` file inside the project subfolder: `src/test/resources/features/{projectName}/`.
*   **Tagging Convention**: Apply project/regression tag groups (`@{PROJECT}_REGRESSION`), a TMS link when applicable (`@tmsLink=...`), and an Allure severity marker (`@severity=...`).
*   Keep scenario steps parameter-driven via `DataTable` or inline arguments — avoid hardcoding values directly in step text; prefer TestData-backed lookups (e.g. `I login as "regularuser" user`).
*   Before adding a new step, check whether an equivalent step already exists elsewhere under `src/test/java/com/dbvi/automation/steps/` (Cucumber glue is scanned repo-wide from `com.dbvi.automation.steps`) — a duplicate step-text regex across two classes causes an ambiguous-step failure at runtime.

*Example:*
```gherkin
Feature: Member Portal Login

  @MMS_REGRESSION @tmsLink=MMS-1001 @severity=Critical
  Scenario: Verify a registered member can log in successfully
    Given I navigate to the environment home page
    When I click on the login link
    And I login as "member" user
    Then I should be logged in successfully
```

### 🏛️ Phase 2: Single-Class Page Object Model (POM) Design
Create the page class inside the project package: `src/main/java/com/dbvi/automation/pages/{projectName}/`.
*   Extend `com.dbvi.automation.pages.BasePage` to inherit the thread-safe active Playwright `Page` and the fluent `webAction` wrapper.
*   Implement verification methods using AssertJ with clear `.withFailMessage(...)` diagnostics.

*Example:*
```java
package com.dbvi.automation.pages.example;

import com.dbvi.automation.pages.BasePage;
import org.assertj.core.api.Assertions;

public class LoginPage extends BasePage {
    private static final String EMAIL_INPUT = "#email";
    private static final String PASSWORD_INPUT = "#Password";
    private static final String LOGIN_BUTTON = "button[type='submit']";
    private static final String LOGGED_IN_AVATAR = "span.symbol-label";

    public LoginPage login(String username, String password) {
        webAction.enterText(EMAIL_INPUT, username);
        webAction.enterText(PASSWORD_INPUT, password);
        webAction.click(LOGIN_BUTTON);
        return this;
    }

    public void verifyLoginSuccessful() {
        Assertions.assertThat(webAction.isElementVisible(LOGGED_IN_AVATAR, 15))
                .withFailMessage("Expected the logged-in member avatar to be visible after login.")
                .isTrue();
    }
}
```

### 🧪 Phase 3: Step Definition Mapping
Create the steps class inside the project package: `src/test/java/com/dbvi/automation/steps/{projectName}/`.
*   Instantiate project Page Objects as parameterless fields at the top of the class.
*   Delegate every action and AssertJ validation directly to the Page Objects — keep the class itself completely decoupled and **assertion-free**.

### 📊 Phase 4: Dynamic Test-Data Resolution
Never hardcode credentials or test inputs. Resolve them using one of two mechanisms:
1.  **Dynamic JDBC Database Fetching**: `UserCredentialsProvider.getUserCredentials(userType)` to dynamically lock a user account for the run (auto-unlocked at teardown via `DriverFactory.quitPlaywright()`).
2.  **Dot-Notation YAML Fallback**: Static properties from the project's environment YAML file (`src/test/resources/env/{projectName}/{envName}/testdata.yaml`):
    ```java
    String username = TestData.get("users.regularuser.username", String.class);
    ```

---

## 📈 Quality & Verification Checklist

Every new test case contribution **must** complete this checklist before check-in:

*   [ ] **Compile Check**: `mvn clean test-compile` — zero compilation or AspectJ weaving failures.
*   [ ] **Zero Hardcoded Sleeps Check**: No `Thread.sleep` or raw `page.waitForTimeout` added; all waiting goes through `WebAction`.
*   [ ] **Assertion Location Check**: No `Assert*` calls inside any `*Steps.java` class.
*   [ ] **Clean Git Tree Check**: `git status` shows no untracked build artifacts (`target/`, `.idea/`) staged.
*   [ ] **No Secrets Check**: If `config.properties`, `reportportal.properties`, or any file under `env/` was touched, confirm no real credential/token/endpoint was reintroduced — use placeholders (`YOUR_...`) for anything secret-shaped.
*   [ ] **Targeted Run**: Prefer `mvn test -Dcucumber.filter.tags="@YOUR_TAG" -Dheadless=true` (with `-Dproject-name=...` / `-Denv=...` / `-Durl=...` overrides as needed) over a full-suite run.
