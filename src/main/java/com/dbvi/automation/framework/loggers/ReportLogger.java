/**
 * COPYRIGHT (C) DBVI, INC. ALL RIGHTS RESERVED. THIS SOFTWARE IS THE CONFIDENTIAL AND PROPRIETARY
 * INFORMATION OF DBVI. ANY DUPLICATION OR USAGE OUTSIDE THE NEEDS OF DBVI IS PROHIBITED.
 */
package com.dbvi.automation.framework.loggers;

import com.dbvi.automation.framework.config.FrameworkProperties;
import com.epam.reportportal.service.ReportPortal;
import io.qameta.allure.Allure;
import java.util.Calendar;
import org.testng.Reporter;

/**
 * Report logger which logs the data to Allure and TestNG
 *
 * @author DbviTestAutomationTeam
 */
public class ReportLogger {

    /**
     * Data logger. Use this only for test data
     *
     * @param logData
     */
    public static void logData(String logData) {
        Reporter.log(logData);
        try {
            ReportPortal.emitLog(logData, "INFO", Calendar.getInstance().getTime());
        } catch (Exception e) {
            // Fallback if ReportPortal is not active or initialized
        }
        Allure.addAttachment("TestData", logData);
        ConsoleLogger(logData);
    }

    /**
     * Log information other than testdata
     *
     * @param info
     */
    public static void logInfo(String info) {
        Reporter.log(info);
        try {
            ReportPortal.emitLog(info, "INFO", Calendar.getInstance().getTime());
        } catch (Exception e) {
            // Fallback if ReportPortal is not active or initialized
        }
        Allure.addAttachment("InfoData", info);
        ConsoleLogger(info);
    }

    /**
     * Log testng step
     *
     * @param stepDetails
     */
    public static void logStep(String stepDetails) {
        Reporter.log(stepDetails);
        try {
            ReportPortal.emitLog(stepDetails, "INFO", Calendar.getInstance().getTime());
        } catch (Exception e) {
            // Fallback if ReportPortal is not active or initialized
        }
        Allure.addAttachment("StepDetails", stepDetails);
        ConsoleLogger(stepDetails);
    }

    private static void ConsoleLogger(String logData) {
        if (FrameworkProperties.isConsoleLog()) {
            ConsoleLogger.logData(logData);
        }
    }
}
