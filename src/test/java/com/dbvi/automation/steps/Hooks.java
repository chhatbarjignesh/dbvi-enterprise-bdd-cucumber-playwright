package com.dbvi.automation.steps;

import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.factory.DriverFactory;
import com.dbvi.automation.framework.loggers.ReportLogger;
import com.dbvi.automation.framework.perfecto.PerfectoReporter;
import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {
    private PerfectoReporter reporter;

    @Before
    public void setup(Scenario scenario) {
        DriverFactory.initPlaywright(FrameworkProperties.getBrowserName());

        // Report test start to Perfecto Smart Reporting natively via Page.evaluate()
        if (FrameworkProperties.isPerfectoEnabled()) {
            reporter = new PerfectoReporter(DriverFactory.getPage());
            DriverFactory.setPerfectoReporter(reporter); // Store in ThreadLocal for step logging
            reporter.testStart(
                    scenario.getName(), new java.util.ArrayList<>(scenario.getSourceTagNames()));
        }
    }

    @AfterStep
    public void afterStep(Scenario scenario) {
        if (FrameworkProperties.isScreenshotAfterStep()) {
            Page page = DriverFactory.getPage();
            if (page != null) {
                byte[] screenshot =
                        page.screenshot(
                                new Page.ScreenshotOptions()
                                        .setFullPage(FrameworkProperties.isScreenshotFullPage()));
                scenario.attach(screenshot, "image/png", "Step Screenshot");
            }
        }
    }

    @After
    public void tearDown(Scenario scenario) {
        Page page = DriverFactory.getPage();

        // Report test completion status to Perfecto Smart Reporting natively via Page.evaluate()
        if (FrameworkProperties.isPerfectoEnabled() && reporter != null) {
            reporter.testEnd(!scenario.isFailed(), scenario.isFailed() ? "Scenario failed" : null);
        }

        if (scenario.isFailed() && page != null) {
            byte[] screenshot =
                    page.screenshot(
                            new Page.ScreenshotOptions()
                                    .setFullPage(FrameworkProperties.isScreenshotFullPage()));
            scenario.attach(screenshot, "image/png", scenario.getName());
        }

        // Capture the client-side jobName before closing the driver
        String jobName = (reporter != null) ? reporter.getJobName() : null;

        // BDD test completion and driver session MUST be fully closed before querying the finalized
        // report
        DriverFactory.quitPlaywright();

        // Query Perfecto's Smart Reporting Public API using our Job Name to retrieve the correct,
        // finalized video report URL
        if (FrameworkProperties.isPerfectoEnabled() && jobName != null) {
            String reportUrl = PerfectoReporter.getFinalizedPerfectoReportUrl(jobName);
            System.out.println("Perfecto Finalized Report URL: " + reportUrl);
            if (reportUrl != null && !reportUrl.isEmpty() && !reportUrl.equals("null")) {
                ReportLogger.logInfo("<a href=\"" + reportUrl + "\">Perfecto Report</a>");
                io.qameta.allure.Allure.getLifecycle()
                        .addAttachment(
                                "Perfecto Report", "text/uri-list", "", reportUrl.getBytes());
            } else {
                System.out.println(
                        "Warning: Perfecto Report URL could not be retrieved from the public API.");
            }
        }
    }
}
