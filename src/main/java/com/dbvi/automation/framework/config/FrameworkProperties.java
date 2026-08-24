package com.dbvi.automation.framework.config;

import java.io.InputStream;
import java.util.Properties;

public class FrameworkProperties {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input =
                FrameworkProperties.class
                        .getClassLoader()
                        .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return value != null ? value : defaultValue;
    }

    public static boolean isBrowserTest() {
        return Boolean.parseBoolean(getProperty("browserTest", "true"));
    }

    public static String getBrowserName() {
        return getProperty("browserName", "chromium");
    }

    public static String getEnvironment() {
        return getProperty("env", "QA");
    }

    public static String getProjectName() {
        return getProperty("project-name", "dsp");
    }

    public static String getAppUrl() {
        return getProperty("url", "https://example.com");
    }

    public static int getPlaywrightTimeout() {
        try {
            return Integer.parseInt(getProperty("playwright.timeout", "60"));
        } catch (NumberFormatException e) {
            return 60; // standard fallback
        }
    }

    public static boolean isMweb() {
        return Boolean.parseBoolean(getProperty("mweb", "false"));
    }

    public static boolean isReadCredentialsFromDb() {
        return Boolean.parseBoolean(getProperty("read-credentials-from-db", "false"));
    }

    public static boolean isRerunEnabled() {
        return Boolean.parseBoolean(getProperty("rerun.enabled", "true"));
    }

    public static boolean isScreenshotAfterStep() {
        return Boolean.parseBoolean(getProperty("screenshot.afterStep", "false"));
    }

    public static boolean isScreenshotFullPage() {
        return Boolean.parseBoolean(getProperty("screenshot.fullPage", "false"));
    }

    public static boolean isConsoleLog() {
        return Boolean.parseBoolean(getProperty("consoleLog", "true"));
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(getProperty("headless", "false"));
    }

    public static boolean isPerfectoEnabled() {
        return Boolean.parseBoolean(getProperty("perfecto.enabled", "false"));
    }

    public static String getPerfectoUrl() {
        return getProperty("perfecto.url", "");
    }

    public static String getPerfectoToken() {
        return getProperty("perfecto.token", "");
    }

    public static String getPerfectoPlatformName() {
        return getProperty("perfecto.platformName", "Windows");
    }

    public static String getPerfectoPlatformVersion() {
        return getProperty("perfecto.platformVersion", "11");
    }

    public static String getPerfectoBrowserName() {
        return getProperty("perfecto.browserName", "Chrome");
    }

    public static String getPerfectoBrowserVersion() {
        return getProperty("perfecto.browserVersion", "latest");
    }

    public static String getPerfectoJobName() {
        String jenkinsJobName = System.getenv("JOB_NAME");
        if (jenkinsJobName != null && !jenkinsJobName.trim().isEmpty()) {
            return jenkinsJobName.trim();
        }
        return getProperty("perfecto.jobName", "Playwright BDD Job");
    }

    public static String getPerfectoProjectName() {
        return getProperty("perfecto.projectName", getProjectName());
    }

    public static int getPerfectoJobNumber() {
        String jenkinsBuildNum = System.getenv("BUILD_NUMBER");
        if (jenkinsBuildNum != null && !jenkinsBuildNum.trim().isEmpty()) {
            try {
                return Integer.parseInt(jenkinsBuildNum.trim());
            } catch (NumberFormatException e) {
                // Fallback
            }
        }
        try {
            return Integer.parseInt(getProperty("perfecto.jobNumber", "1"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static String getResolution() {
        return getProperty("resolution", "1920x1080");
    }

    public static boolean isGridEnabled() {
        return Boolean.parseBoolean(getProperty("grid.enabled", "false"));
    }

    public static String getGridUrl() {
        return getProperty("grid.url", "http://b1q1-lqcnap03.dbviinc.lcl:4444");
    }

    public static String getSsoUsername() {
        return getProperty("sso.username", "your_sso_username@dbvi.com");
    }

    public static String getSsoPassword() {
        return getProperty("sso.password", "your_sso_password");
    }

    public static String getSsoTotpSecret() {
        return getProperty("sso.totpSecret", "BP26 TDZU Z5SV PZJR");
    }

    public static String getDbUrl() {
        return getProperty(
                "db.url", "jdbc:postgresql://CLD1-WQCNDB01.dbviinc.lcl:5432/QATESTAUTOMATION");
    }

    public static String getDbUsername() {
        return getProperty("db.username", "testautomation");
    }

    public static String getDbPassword() {
        try {
            return com.dbvi.automation.framework.utils.EncryptionUtil.decrypt(
                    getProperty("db.password", "YOUR_ENCRYPTED_DB_PASSWORD"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt database password!", e);
        }
    }

    public static String getPerfectoTestContext() {
        return getProperty("perfecto.test-context", "Regression");
    }

    public static String getPerfectoLocation() {
        return getProperty("perfecto.location", "US East");
    }

    public static String getEmailRecipients() {
        return getProperty("email.recipients", "ecomqa@dbvi.com");
    }

    public static String getEmailCC() {
        return getProperty("email.cc", "");
    }
}
