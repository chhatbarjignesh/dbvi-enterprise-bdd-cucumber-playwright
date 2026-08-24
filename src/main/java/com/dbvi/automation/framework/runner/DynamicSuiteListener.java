package com.dbvi.automation.framework.runner;

import com.dbvi.automation.framework.config.FrameworkProperties;
import java.util.ArrayList;
import java.util.List;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

/**
 * DynamicSuiteListener is an enterprise TestNG suite interceptor. Instead of altering classes
 * inside a single test tag, it dynamically appends a second <test> tag sequentially. This ensures
 * that TestNG delays the rerun's data provider evaluation until AFTER the primary tests have run
 * and populated rerun.txt, guaranteeing perfect ReportPortal launch status healing!
 */
public class DynamicSuiteListener implements IAlterSuiteListener {

    @Override
    public void alter(List<XmlSuite> suites) {
        boolean rerunEnabled = FrameworkProperties.isRerunEnabled();
        System.out.println(
                "=== TestNG Dynamic Suite Interceptor: rerun.enabled = " + rerunEnabled + " ===");

        // Pre-create an empty rerun.txt file on-the-fly to prevent Cucumber 7 from crashing during
        // class setup
        if (rerunEnabled) {
            try {
                java.nio.file.Files.createDirectories(
                        java.nio.file.Paths.get("target/cucumber-reports/rerun-reports"));
                java.io.File rerunFile =
                        new java.io.File("target/cucumber-reports/rerun-reports/rerun.txt");
                if (!rerunFile.exists()) {
                    rerunFile.createNewFile();
                }
            } catch (Exception e) {
                System.out.println(
                        "Warning: Failed to pre-create rerun.txt file: " + e.getMessage());
            }
        }

        for (XmlSuite suite : suites) {
            List<XmlTest> tests = new ArrayList<>();

            // 1. Create and add the first <test> block: Primary BDD Run
            XmlTest primaryTest = new XmlTest(suite);
            primaryTest.setName("BDD Tests");
            List<XmlClass> primaryClasses = new ArrayList<>();
            primaryClasses.add(
                    new XmlClass("com.dbvi.automation.framework.runner.RunCucumberTest"));
            primaryTest.setXmlClasses(primaryClasses);
            tests.add(primaryTest);

            // 2. Conditionally append the second <test> block sequentially: Auto-Healing Rerun
            if (rerunEnabled) {
                System.out.println(
                        "TestNG Suite: Appending separate <test> block for ReRunCucumberTest.");
                XmlTest rerunTest = new XmlTest(suite);
                rerunTest.setName("BDD Reruns");
                List<XmlClass> rerunClasses = new ArrayList<>();
                rerunClasses.add(
                        new XmlClass("com.dbvi.automation.framework.runner.ReRunCucumberTest"));
                rerunTest.setXmlClasses(rerunClasses);
                tests.add(rerunTest);
            } else {
                System.out.println("TestNG Suite: Excluding ReRunCucumberTest from suite.");
            }

            // Override the suite's tests with our dynamically assembled list (delayed evaluation
            // active!)
            suite.setTests(tests);
        }
    }
}
