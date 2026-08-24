package com.dbvi.automation.framework.runner;

import com.dbvi.automation.framework.utils.AllureReportHelper;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.DataProvider;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.dbvi.automation.steps"},
        plugin = {
            "pretty",
            "html:target/cucumber-reports/cucumber.html",
            "json:target/cucumber-reports/cucumber.json",
            "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
            "com.epam.reportportal.cucumber.ScenarioReporter",
            "com.dbvi.automation.framework.perfecto.PerfectoReportingPlugin",
            "rerun:target/cucumber-reports/rerun-reports/rerun.txt"
        },
        monochrome = true)
public class RunCucumberTest extends AbstractTestNGCucumberTests {
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    @AfterSuite
    public void teardownSuite() {
        AllureReportHelper.writeAllureEnvironmentFile();
        AllureReportHelper.writeAllureCategoriesFile();
    }
}
