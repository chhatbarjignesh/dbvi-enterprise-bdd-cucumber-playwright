package com.dbvi.automation.framework.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * ReRunCucumberTest re-executes strictly the failed scenarios written in rerun.txt. It is
 * dynamically appended as a separate Test block inside the TestNG suite to ensure its DataProvider
 * is only evaluated after the primary run has completed.
 */
@CucumberOptions(
        features = "@target/cucumber-reports/rerun-reports/rerun.txt",
        glue = {"com.dbvi.automation.steps"},
        plugin = {
            "pretty",
            "html:target/cucumber-reports/rerun-reports/rerun.html",
            "json:target/cucumber-reports/rerun-reports/rerun.json",
            "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
            "com.epam.reportportal.cucumber.ScenarioReporter",
            "com.dbvi.automation.framework.perfecto.PerfectoReportingPlugin",
            "rerun:target/cucumber-reports/rerun-reports/rerun2.txt"
        },
        monochrome = true)
public class ReRunCucumberTest extends AbstractTestNGCucumberTests {

    @org.testng.annotations.BeforeClass
    public void setupRerun() {
        System.setProperty("rp.rerun", "true");
        System.setProperty("rp.rerun.of", "latest");
        System.out.println("=== ReportPortal Rerun Mode Activated: rp.rerun=true ===");
    }

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
