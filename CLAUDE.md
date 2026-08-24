# CLAUDE.md — Dbvi Playwright Cucumber BDD Framework

This file is the repo-wide mandate for Claude Code (or any AI assistant) working in this codebase: how to develop new BDD test cases, step definitions, and Page Objects, and the conventions that must not be violated.

> See [FRAMEWORK_MAP.md](FRAMEWORK_MAP.md) for the full directory map, package layout, and layered architecture diagrams. See [README.md](README.md) for setup, configuration, and CLI usage.
>
> This repo ships two working project modules: `example` (`pages/example/HomePage.java` + `steps/ExampleSteps.java` + `test.feature` + `env/example/`), the framework's own minimal reference/self-test, and `dbvi` (`pages/dbvi/LoginPage.java` + `steps/dbvi/LoginSteps.java` + `features/dbvi/login.feature` + `env/dbvi/`), a real scenario testing the MMS Member Portal login. The prior `dsp`/`events` business modules were removed as client-specific. `{projectName}` below is a placeholder — substitute your own module name, following either module's layout as your template.

---

## Core architectural principles (non-negotiable)

1. **Pure Playwright engine** — never introduce Selenium or Appium dependencies. All browser interaction goes through Playwright's native protocol.
2. **Multi-project module isolation** — every business application/module gets its own folder name, reused consistently across four locations:
   - Feature files: `src/test/resources/features/{projectName}/`
   - Step definitions: `src/test/java/com/dbvi/automation/steps/{projectName}/`
   - POM pages: `src/main/java/com/dbvi/automation/pages/{projectName}/`
   - Test data YAML: `src/test/resources/env/{projectName}/{envName}/testdata.yaml`
3. **Single-Class Page Object Model** — selectors (`private static final String` constants), actions, and assertions all live in one Page class. Never split into separate page/locator/action files.
4. **Assertion-free step definitions** — step definitions must never contain `Assert.`, `assertThat`, or `assertEquals`. All AssertJ validation lives inside Page Object methods.
5. **Zero-flakiness `WebAction` wrapper** — never write `Thread.sleep` or raw `page.waitForTimeout` in page methods. Always go through the `webAction` instance (inherited from `BasePage`), which enforces wait-for-visibility before every interaction. If a click is intercepted by a sticky header or overlay, use `webAction.clickUsingJS(selector)` instead of adding a wait/retry loop.

---

## Workflow for adding a new BDD test case

### 1. Gherkin feature file
Create `src/test/resources/features/{projectName}/*.feature`. Tag every scenario with the project/regression group (`@{PROJECT}_REGRESSION`), a TMS link if applicable (`@tmsLink=...`), and an Allure severity (`@severity=...`). Keep scenario steps parameter-driven via `DataTable` or inline arguments rather than hardcoding values in step text.

### 2. Page Object (Single-Class POM)
Create `src/main/java/com/dbvi/automation/pages/{projectName}/YourPage.java` extending `com.dbvi.automation.pages.BasePage`. Selectors as constants at the top; fluent action methods; AssertJ verification methods with `.withFailMessage(...)` for clear failure diagnostics. See `pages/example/HomePage.java` for the minimal reference pattern.

### 3. Step definitions
Create `src/test/java/com/dbvi/automation/steps/{projectName}/YourSteps.java`. Instantiate Page Objects as parameterless fields, delegate every action and assertion to them, and keep the class itself free of any validation logic. See `steps/ExampleSteps.java` for the reference pattern.

### 4. Test data resolution
Never hardcode credentials or test inputs. Two mechanisms are available:
- **Dynamic JDBC**: `UserCredentialsProvider.getUserCredentials(userType)` locks the DB row and auto-unlocks it at teardown via `DriverFactory.quitPlaywright()`.
- **YAML fallback**: `TestData.get("users.regularuser.username", String.class)` reads from `env/{project}/{env}/testdata.yaml`, dot-notation traversal.

---

## Before calling a change done

Run these checks (via the Bash tool) before reporting any test-case or framework change complete:

- [ ] `mvn clean test-compile` — zero compilation or AspectJ weaving failures.
- [ ] No `Thread.sleep` / raw `page.waitForTimeout` introduced in page methods — use `WebAction` waits.
- [ ] No `Assert*` calls inside any `*Steps.java` class.
- [ ] `git status` shows no untracked build artifacts (`target/`, `.idea/`) staged.
- [ ] If you touched `config.properties`, `reportportal.properties`, or any file under `env/`, confirm you didn't reintroduce a real credential/token/endpoint — use placeholders (`YOUR_...`) for anything secret-shaped. See [FRAMEWORK_MAP.md § Security Notes](FRAMEWORK_MAP.md#5-security-notes).
- [ ] For a real scenario run, prefer `mvn test -Dcucumber.filter.tags="@YOUR_TAG" -Dheadless=true` over a full-suite run.

---

## Things to avoid

- Don't add a second assertion library, HTTP client, or browser-automation dependency when `AssertJ` / `REST Assured` / Playwright already cover the need.
- Don't invent a new config-loading mechanism — extend `FrameworkProperties` and reuse `getProperty(key, default)`.
- Don't rename `com.dbvi.automation` package roots or the Maven `groupId`/`artifactId` without updating `pom.xml`, `testng.xml`, `testng-rerun.xml`, and every `glue`/`XmlClass` string in the runner classes — they're not auto-derived.
- Don't recreate `dsp`/`events`-style client-specific business scenarios in this repo; add new scenarios under a new, generically-named project folder instead.
