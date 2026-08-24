package com.dbvi.automation.framework.factory;

import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.loggers.ReportLogger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DriverHelper is a package-private utility class that contains refactored, reusable helper methods
 * to assist DriverFactory in spawning Playwright instances, configuring environments, and executing
 * local/grid/cloud browser connects cleanly (DRY).
 */
class DriverHelper {
    private static final Logger logger = LoggerFactory.getLogger(DriverHelper.class);

    /** Builds Playwright's system environment and creation options. */
    static Playwright.CreateOptions buildCreateOptions() {
        Playwright.CreateOptions createOptions = new Playwright.CreateOptions();
        Map<String, String> env = new HashMap<>(System.getenv());

        if (FrameworkProperties.isPerfectoEnabled() || FrameworkProperties.isGridEnabled()) {
            // Skip browser downloads for cloud/grid runs since they run in remote containers
            env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        } else {
            env.remove("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD");
        }

        if (FrameworkProperties.isGridEnabled()) {
            env.put("SELENIUM_REMOTE_URL", FrameworkProperties.getGridUrl());
        }

        createOptions.setEnv(env);
        return createOptions;
    }

    /** Handshakes and establishes a remote browser connection to the Perfecto Cloud. */
    static Browser connectToPerfecto(Playwright playwright) {
        String pBrowserName = FrameworkProperties.getPerfectoBrowserName();
        ReportLogger.logInfo(
                "Perfecto cloud run enabled. Instantiating remote "
                        + pBrowserName
                        + " browser on platform: "
                        + FrameworkProperties.getPerfectoPlatformName()
                        + " "
                        + FrameworkProperties.getPerfectoPlatformVersion());

        try {
            JsonObject capabilities = new JsonObject();
            capabilities.addProperty("browserName", pBrowserName);
            capabilities.addProperty(
                    "browserVersion", FrameworkProperties.getPerfectoBrowserVersion());
            capabilities.addProperty("platformName", FrameworkProperties.getPerfectoPlatformName());
            capabilities.addProperty(
                    "platformVersion", FrameworkProperties.getPerfectoPlatformVersion());
            capabilities.addProperty("securityToken", FrameworkProperties.getPerfectoToken());
            capabilities.addProperty("location", FrameworkProperties.getPerfectoLocation());
            capabilities.addProperty("report.jobName", FrameworkProperties.getPerfectoJobName());
            capabilities.addProperty(
                    "report.jobNumber", FrameworkProperties.getPerfectoJobNumber());

            String resolution = FrameworkProperties.getResolution();
            if (resolution != null && !resolution.trim().isEmpty() && resolution.contains("x")) {
                capabilities.addProperty("resolution", resolution.trim());
            }

            if (!FrameworkProperties.isMweb()) {
                capabilities.addProperty("windowState", "maximize");
            }

            String encodedCaps =
                    URLEncoder.encode(capabilities.toString(), StandardCharsets.UTF_8.name());
            String wssUrl = FrameworkProperties.getPerfectoUrl() + "?" + encodedCaps;

            Browser browser;
            String pBrowserNameLower = pBrowserName.toLowerCase();
            if (pBrowserNameLower.contains("firefox")) {
                browser = playwright.firefox().connect(wssUrl);
            } else if (pBrowserNameLower.contains("safari")
                    || pBrowserNameLower.contains("webkit")) {
                browser = playwright.webkit().connect(wssUrl);
            } else {
                browser = playwright.chromium().connect(wssUrl);
            }

            ReportLogger.logInfo("Playwright Connected Browser Version: " + browser.version());
            return browser;

        } catch (Exception e) {
            logger.error("Failed to establish remote connection to Perfecto cloud", e);
            throw new RuntimeException(
                    "Failed to initialize remote Playwright connection to Perfecto", e);
        }
    }

    /** Spawns a local browser or handshakes with a remote Selenium Grid hub. */
    static Browser launchLocalOrGridBrowser(Playwright playwright, String browserName) {
        boolean headless = FrameworkProperties.isHeadless();
        boolean gridEnabled = FrameworkProperties.isGridEnabled();

        if (gridEnabled) {
            ReportLogger.logInfo(
                    "Selenium Grid run enabled. Directing execution to hub: "
                            + FrameworkProperties.getGridUrl());
        } else {
            ReportLogger.logInfo(
                    "Local run enabled. Launching "
                            + browserName
                            + " browser (headless: "
                            + headless
                            + ")");
        }

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);

        // Natively maximize the browser for local desktop executions
        if (!FrameworkProperties.isMweb()) {
            List<String> args = new ArrayList<>();
            args.add("--start-maximized");
            options.setArgs(args);
        }

        // Since the Selenium Grid only has Chrome support, force Chromium connection
        String targetBrowser = gridEnabled ? "chromium" : browserName;

        switch (targetBrowser.toLowerCase()) {
            case "firefox":
                return playwright.firefox().launch(options);
            case "webkit":
                return playwright.webkit().launch(options);
            case "chrome":
                return playwright.chromium().launch(options.setChannel("chrome"));
            case "chromium":
            default:
                return playwright.chromium().launch(options);
        }
    }

    /** Builds and configures viewport size and mobile emulation settings for BrowserContext. */
    static Browser.NewContextOptions buildContextOptions() {
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();
        boolean mwebRun = FrameworkProperties.isMweb();

        if (mwebRun) {
            String resolution = FrameworkProperties.getResolution();
            int width = 375;
            int height = 812;
            if (resolution != null && resolution.contains("x")) {
                try {
                    String[] parts = resolution.split("x");
                    width = Integer.parseInt(parts[0].trim());
                    height = Integer.parseInt(parts[1].trim());
                } catch (Exception e) {
                    // Safe fallback
                }
            }
            contextOptions.setViewportSize(width, height);
            ReportLogger.logInfo(
                    "Mobile Web (mweb) active. Non-maximized viewport size configured to: "
                            + width
                            + "x"
                            + height);
        } else {
            // Desktop run: natively maximize by disabling default 1280x720 override
            contextOptions.setViewportSize(null);
            ReportLogger.logInfo(
                    "Local Desktop run active. Context viewport size configured to null for native maximization.");
        }
        return contextOptions;
    }
}
