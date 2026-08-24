/**
 * COPYRIGHT (C) DBVI, INC. ALL RIGHTS RESERVED. THIS SOFTWARE IS THE CONFIDENTIAL AND
 * PROPRIETARY INFORMATION OF DBVI. ANY DUPLICATION OR USAGE OUTSIDE THE NEEDS OF DBVI
 * IS PROHIBITED.
 */
package com.dbvi.automation.framework.loggers;

/**
 * Console Logger class. Contains methods to output to console in different formats
 *
 * @author DbviTestAutomationTeam
 */
public class ConsoleLogger {
    /**
     * Console logger
     *
     * @param logData
     */
    public static void logData(String logData) {
        System.out.println(logData);
    }
}
