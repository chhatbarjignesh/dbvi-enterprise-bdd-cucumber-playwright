# Dbvi Playwright BDD Framework Standards Reference

This document outlines the package mappings, directory locations, and configuration property schemas required to develop and execute test suites in this workspace. See `FRAMEWORK_MAP.md` at the repo root for the full architectural breakdown — this is a condensed, skill-scoped excerpt.

---

## 📂 Framework Directory & Package Mapping

All assets are separated by project name (e.g. `example`, `checkout`) to ensure multi-project isolation:

```text
src/
├── main/java/com/dbvi/automation/
│     ├── framework/
│     │     ├── config/FrameworkProperties.java       # Central properties loader (system prop > config.properties > default)
│     │     ├── factory/
│     │     │     ├── DriverFactory.java               # ThreadLocal Playwright/Browser/Context/Page + active credentials
│     │     │     └── DriverHelper.java                # Local/Grid/Perfecto browser bring-up
│     │     ├── utils/
│     │     │     ├── EncryptionUtil.java              # PBEWithMD5AndDES encrypt/decrypt
│     │     │     ├── TestData.java                    # Dot-notation YAML lookup
│     │     │     ├── TotpUtil.java                    # Google Authenticator TOTP code generation
│     │     │     └── testdata/
│     │     │           └── UserCredentialsProvider.java # DB user locking lifecycle manager
│     │     └── wrapper/ui/WebAction.java               # Wait-to-click Playwright wrappers
│     └── pages/
│           ├── BasePage.java                           # Shared Base Page Object
│           └── {project}/                              # Project-specific isolated Page Objects
└── test/
    ├── java/com/dbvi/automation/
    │     ├── runner/RunCucumberTest.java               # Central TestNG parallel runner (glue = com.dbvi.automation.steps)
    │     └── steps/{project}/                          # Project-specific isolated Step Definitions
    └── resources/
          ├── config.properties                          # Global properties configurations
          ├── allure.properties                          # Unifies allure results directory
          ├── testng.xml                                 # TestNG parallel suite definitions
          ├── features/{project}/                        # Project-specific Gherkin Feature files
          └── env/{project}/{env}/testdata.yaml          # Project-specific environment test data
```

---

## ⚙️ Configuration Properties Schema

Reference list of key configurations available inside `config.properties` (all overridable via `-D<key>=<value>` on the Maven command line):

| Key Name | Default Value | Description |
| :--- | :--- | :--- |
| `browserName` | `chromium` | Target browser for execution (chromium, firefox, webkit) |
| `headless` | `false` | Enable/Disable headless execution |
| `project-name` | `example` | Active target project module namespace |
| `env` | `QA1` | Target environment profile (QA1, QA, UAT) |
| `url` | — | Base application URL used by `FrameworkProperties.getAppUrl()` |
| `playwright.timeout` | `60` | Global action and navigation timeout limit (seconds) |
| `mweb` | `false` | Enable/Disable Mobile Web emulation |
| `resolution` | `1920x1080` | Virtual resolution size for remote VM viewports |
| `grid.enabled` | `false` | Enable/Disable remote Selenium Grid execution |
| `perfecto.enabled` | `false` | Enable/Disable remote Perfecto Cloud execution |
| `read-credentials-from-db`| `true` | Enable/Disable PostgreSQL DB user locking lifecycle |

---

## 🛡️ Database User Locking & Credentials Schema

When `read-credentials-from-db=true` is enabled, the framework:
1. Connects to the Postgres instance configured via `db.url` / `db.username` / `db.password` in `config.properties` (password is stored encrypted; never hardcode a plaintext or decrypted credential in any tracked file — see `CLAUDE.md` § Before calling a change done).
2. Queries `"EXECUTION".userdetails` to find an unlocked user (`lockedstatus = 'N'`) matching the active environment and userType.
3. Automatically locks the user by setting `lockedstatus = 'Y'` and `lockedby = '{hostname}'`.
4. Registers the locked user in thread-local storage, automatically unlocking it (`lockedstatus = 'N'`, `lockedby = 'N'`) in `DriverFactory.quitPlaywright()` upon test teardown.
