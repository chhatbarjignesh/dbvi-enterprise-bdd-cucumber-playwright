package com.dbvi.automation.framework.factory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.loggers.ReportLogger;
import com.dbvi.automation.framework.perfecto.PerfectoReporter;
import com.dbvi.automation.framework.utils.testdata.UserCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DriverFactory manages thread-local storage of Playwright instances, browsers, contexts, and
 * pages. It leverages the package-private DriverHelper to delegate low-level browser setups and
 * maintain clean DRY principles.
 */
public class DriverFactory {
    private static final Logger logger = LoggerFactory.getLogger(DriverFactory.class);

    private static ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<PerfectoReporter> reporterThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<UserCredentialsProvider.UserCredentials>
            activeCredentialsThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> activeUserTypeThreadLocal = new ThreadLocal<>();

    public static PerfectoReporter getPerfectoReporter() {
        return reporterThreadLocal.get();
    }

    public static void setPerfectoReporter(PerfectoReporter reporter) {
        reporterThreadLocal.set(reporter);
    }

    public static UserCredentialsProvider.UserCredentials getActiveCredentials() {
        return activeCredentialsThreadLocal.get();
    }

    public static void setActiveCredentials(
            UserCredentialsProvider.UserCredentials creds, String userType) {
        activeCredentialsThreadLocal.set(creds);
        activeUserTypeThreadLocal.set(userType);
    }

    /**
     * Spawns, configures, and registers the thread-safe active Playwright session for local,
     * Selenium Grid, or Perfecto Cloud runs.
     */
    public static void initPlaywright(String browserName) {
        ReportLogger.logInfo("Spawning local Playwright process...");
        Playwright playwright = Playwright.create(DriverHelper.buildCreateOptions());
        playwrightThreadLocal.set(playwright);

        // Dynamic browser connection delegation (DRY)
        Browser browser =
                FrameworkProperties.isPerfectoEnabled()
                        ? DriverHelper.connectToPerfecto(playwright)
                        : DriverHelper.launchLocalOrGridBrowser(playwright, browserName);
        browserThreadLocal.set(browser);

        ReportLogger.logInfo("Creating a fresh browser context and page instance.");
        BrowserContext context = browser.newContext(DriverHelper.buildContextOptions());
        contextThreadLocal.set(context);

        Page page = context.newPage();

        // Dynamically apply global configurable timeouts
        int timeoutMs = FrameworkProperties.getPlaywrightTimeout() * 1000;
        page.setDefaultTimeout(timeoutMs);
        page.setDefaultNavigationTimeout(timeoutMs);
        ReportLogger.logInfo(
                "Configured Playwright default action and navigation timeouts globally to: "
                        + FrameworkProperties.getPlaywrightTimeout()
                        + " seconds");

        pageThreadLocal.set(page);
        ReportLogger.logInfo("Playwright session successfully initialized.");
    }

    public static Page getPage() {
        return pageThreadLocal.get();
    }

    /** Terminates and cleans up the thread-active Playwright sessions and page instances. */
    public static void quitPlaywright() {
        if (pageThreadLocal.get() != null) {
            ReportLogger.logInfo("Closing active Playwright browser, context, and page instances.");

            // Automatically unlock the database user if credentials were fetched dynamically from
            // DB
            UserCredentialsProvider.UserCredentials creds = activeCredentialsThreadLocal.get();
            String userType = activeUserTypeThreadLocal.get();
            if (creds != null && userType != null) {
                UserCredentialsProvider.unlockUser(creds.getUsername(), userType);
            }
            activeCredentialsThreadLocal.remove();
            activeUserTypeThreadLocal.remove();

            contextThreadLocal.get().close();
            browserThreadLocal.get().close();
            playwrightThreadLocal.get().close();
            pageThreadLocal.remove();
            contextThreadLocal.remove();
            browserThreadLocal.remove();
            playwrightThreadLocal.remove();
            reporterThreadLocal.remove();
            ReportLogger.logInfo("Playwright teardown completed successfully.");
        }
    }
}
