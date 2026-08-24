package com.dbvi.automation.framework.utils;

import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.loggers.ConsoleLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * AllureReportHelper is an enterprise utility to programmatically generate environment.properties
 * and dynamic categories.json files at execution runtime.
 */
public class AllureReportHelper {

    /** Copy categories.json into the target/allure-results folder. */
    public static void writeAllureCategoriesFile() {
        String sourcePath = "src/test/resources/allure/categories.json";
        String targetDir = "target/allure-results";
        String targetPath = targetDir + "/categories.json";

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            ConsoleLogger.logData(
                    "Allure categories.json template not found at "
                            + sourcePath
                            + ". Skipping copy.");
            return;
        }

        try {
            Files.createDirectories(Paths.get(targetDir));
            Files.copy(
                    sourceFile.toPath(),
                    Paths.get(targetPath),
                    StandardCopyOption.REPLACE_EXISTING);
            ConsoleLogger.logData(
                    "Successfully copied Allure categories.json into target/allure-results.");
        } catch (IOException e) {
            ConsoleLogger.logData("Error copying Allure categories.json: " + e.getMessage());
        }
    }

    /** Dynamically write allure environment.properties into target/allure-results. */
    public static void writeAllureEnvironmentFile() {
        String targetDir = "target/allure-results";
        String filePath = targetDir + "/environment.properties";

        try {
            Files.createDirectories(Paths.get(targetDir));
            try (OutputStream output = new FileOutputStream(filePath)) {
                Properties prop = new Properties();

                // ENVIRONMENT / APPLICATION CONFIG
                prop.setProperty("PROJECT NAME", FrameworkProperties.getProjectName());
                prop.setProperty("ENVIRONMENT", FrameworkProperties.getEnvironment());
                prop.setProperty("APPLICATION URL", FrameworkProperties.getAppUrl());

                // RUN CONFIG
                prop.setProperty("BROWSER", FrameworkProperties.getBrowserName());
                prop.setProperty("HEADLESS", Boolean.toString(FrameworkProperties.isHeadless()));
                prop.setProperty("MOBILE WEB RUN?", Boolean.toString(FrameworkProperties.isMweb()));
                prop.setProperty("SCREEN RESOLUTION", FrameworkProperties.getResolution());
                prop.setProperty(
                        "PLAYWRIGHT TIMEOUT LIMIT",
                        Integer.toString(FrameworkProperties.getPlaywrightTimeout()) + "s");

                // DATABASE CONFIG
                prop.setProperty(
                        "READ CREDENTIALS FROM DB?",
                        Boolean.toString(FrameworkProperties.isReadCredentialsFromDb()));
                prop.setProperty("DATABASE URL", FrameworkProperties.getDbUrl());

                // CI/CD DEPLOYMENT CONFIG
                String jenkinsJobName = System.getenv("JOB_NAME");
                String jenkinsBuildNum = System.getenv("BUILD_NUMBER");
                if (jenkinsJobName != null) {
                    prop.setProperty("JENKINS JOB NAME", jenkinsJobName);
                    prop.setProperty("JENKINS BUILD NUMBER", jenkinsBuildNum);
                }

                prop.store(output, "Allure Report Environment Properties");
                ConsoleLogger.logData("Successfully wrote dynamic Allure environment.properties.");
            }
        } catch (IOException io) {
            ConsoleLogger.logData(
                    "Error while writing allure environment properties: " + io.getMessage());
        }
    }
}
