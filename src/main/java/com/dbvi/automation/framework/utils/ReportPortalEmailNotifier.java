package com.dbvi.automation.framework.utils;

import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.loggers.ConsoleLogger;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * ReportPortalEmailNotifier is a utility to fetch test execution statistics and failure details
 * from ReportPortal via REST API, and automatically compile and send a detailed HTML email
 * notification.
 */
public class ReportPortalEmailNotifier {

    public static void main(String[] args) {
        try {
            String imageName = System.getProperty("image.name", "N/A");
            String imageTag = System.getProperty("image.tag", "N/A");
            String branchName = System.getProperty("branch.name", "N/A");
            String executionType = System.getProperty("execution.type", "BST");

            // Load from reportportal.properties file if system properties are omitted (dynamic
            // fallback)
            Properties rpProps = new Properties();
            java.io.File rpFile = new java.io.File("src/test/resources/reportportal.properties");
            if (rpFile.exists()) {
                try (java.io.InputStream is = new java.io.FileInputStream(rpFile)) {
                    rpProps.load(is);
                } catch (Exception e) {
                    // Safe ignore
                }
            }

            String rpProjectId =
                    System.getProperty("rp.project", rpProps.getProperty("rp.project"));

            // Default rpLaunchId to Jenkins JOB_NAME if omitted
            String rpLaunchId = System.getProperty("rp.launch");
            if (rpLaunchId == null || rpLaunchId.trim().isEmpty()) {
                rpLaunchId = System.getenv("JOB_BASE_NAME");
            }
            if (rpLaunchId == null || rpLaunchId.trim().isEmpty()) {
                rpLaunchId = rpProps.getProperty("rp.launch");
            }

            String rpEndpoint =
                    System.getProperty("rp.endpoint", rpProps.getProperty("rp.endpoint"));
            String rpUuid =
                    System.getProperty(
                            "rp.uuid",
                            rpProps.getProperty("rp.api.key", rpProps.getProperty("rp.uuid")));

            String recipients =
                    System.getProperty(
                            "email.recipients", FrameworkProperties.getEmailRecipients());
            String ccRecipients = System.getProperty("email.cc", FrameworkProperties.getEmailCC());
            String mockProp = System.getProperty("mock");
            boolean isMock = "true".equalsIgnoreCase(mockProp);
            String maxFailuresProp = System.getProperty("email.max.failures", "10");
            int maxFailures =
                    "all".equalsIgnoreCase(maxFailuresProp)
                            ? 500
                            : Integer.parseInt(maxFailuresProp);

            ConsoleLogger.logData("ReportPortalEmailNotifier - execution.type: " + executionType);
            ConsoleLogger.logData("ReportPortalEmailNotifier - mock: " + mockProp);
            ConsoleLogger.logData("ReportPortalEmailNotifier - rp.launch.id/name: " + rpLaunchId);
            ConsoleLogger.logData("ReportPortalEmailNotifier - rp.project: " + rpProjectId);
            ConsoleLogger.logData("ReportPortalEmailNotifier - email.recipients: " + recipients);
            ConsoleLogger.logData("ReportPortalEmailNotifier - email.cc: " + ccRecipients);

            int total, passed, failed, skipped;
            String rpLink;

            List<FailedItemDetail> failedItems = new ArrayList<>();

            if (isMock) {
                ConsoleLogger.logData("Running in MOCK mode. Using sample statistics.");
                total = 100;
                passed = 95;
                failed = 3;
                skipped = 2;
                rpLink = "https://reportportal.dbvi.com/ui/#sample-project/launches/all/sample-id";
                failedItems.add(
                        new FailedItemDetail(
                                "User can add item to cart",
                                "Then the cart count should be 1",
                                "AssertionError: expected <0> but was <1>"));
                failedItems.add(
                        new FailedItemDetail(
                                "User can complete checkout",
                                "When the user clicks Place Order",
                                "ElementNotInteractableException: element is not visible"));
                failedItems.add(
                        new FailedItemDetail(
                                "User can search for a product",
                                "Then search results should display 10 items",
                                "AssertionError: expected <10> but was <0>"));
            } else {
                if (rpLaunchId == null
                        || rpEndpoint == null
                        || rpUuid == null
                        || rpProjectId == null) {
                    ConsoleLogger.logData(
                            "Missing required ReportPortal properties (rp.launch, rp.endpoint, rp.uuid, or rp.project).");
                    return;
                }

                // Normalize endpoint (ensure no trailing slash)
                if (rpEndpoint.endsWith("/")) {
                    rpEndpoint = rpEndpoint.substring(0, rpEndpoint.length() - 1);
                }

                // 1. Fetch statistics from ReportPortal
                ConsoleLogger.logData(
                        "Attempting to fetch statistics from ReportPortal for: " + rpLaunchId);
                RequestSpecification requestSpec =
                        RestAssured.given()
                                .contentType("application/json")
                                .header("Authorization", "BEARER " + rpUuid);

                String launchUrl = rpEndpoint + "/api/v1/" + rpProjectId + "/launch/" + rpLaunchId;
                Response response = requestSpec.get(launchUrl);

                // If direct fetch fails with 404, it might be a launch name instead of an ID
                if (response.statusCode() == 404 || !isNumericOrUuid(rpLaunchId)) {
                    ConsoleLogger.logData(
                            "Launch ID not found or looks like a name. Searching for latest launch with name: "
                                    + rpLaunchId);
                    String searchUrl =
                            rpEndpoint
                                    + "/api/v1/"
                                    + rpProjectId
                                    + "/launch?filter.eq.name="
                                    + rpLaunchId
                                    + "&page.sort=id,desc";
                    Response searchResponse = requestSpec.get(searchUrl);

                    if (searchResponse.statusCode() == 200
                            && searchResponse.jsonPath().getList("content").size() > 0) {
                        String resolvedId = searchResponse.jsonPath().getString("content[0].id");
                        ConsoleLogger.logData(
                                "Resolved Launch Name '" + rpLaunchId + "' to ID: " + resolvedId);
                        rpLaunchId = resolvedId;
                        launchUrl = rpEndpoint + "/api/v1/" + rpProjectId + "/launch/" + rpLaunchId;
                        response = requestSpec.get(launchUrl);
                    } else {
                        ConsoleLogger.logData(
                                "Failed to resolve launch name or find launch details. Status code: "
                                        + response.statusCode());
                        return;
                    }
                }

                if (response.statusCode() != 200) {
                    ConsoleLogger.logData(
                            "Failed to fetch launch details from ReportPortal. Status code: "
                                    + response.statusCode());
                    ConsoleLogger.logData("Response: " + response.asString());
                    return;
                }

                total = getIntFromPath(response, "statistics.executions.total");
                passed = getIntFromPath(response, "statistics.executions.passed");
                failed = getIntFromPath(response, "statistics.executions.failed");
                skipped = getIntFromPath(response, "statistics.executions.skipped");
                rpLink = rpEndpoint + "/ui/#" + rpProjectId + "/launches/all/" + rpLaunchId;

                if (failed > 0) {
                    ConsoleLogger.logData(
                            "Fetching failed item details from ReportPortal (max: "
                                    + maxFailures
                                    + ")...");
                    failedItems =
                            fetchFailedItems(
                                    rpEndpoint, rpProjectId, rpUuid, rpLaunchId, maxFailures);
                }
            }

            double passRate = total > 0 ? ((double) passed / total) * 100 : 0;
            String formattedPassRate = String.format("%.2f", passRate);

            // 2. Construct Email
            String subject;
            if ("BST".equalsIgnoreCase(executionType)) {
                subject =
                        String.format(
                                "[BST Execution Report] Branch: %s | Repository: %s | Tag: %s | Pass Rate: %s%%",
                                branchName, imageName, imageTag, formattedPassRate);
            } else {
                subject =
                        String.format(
                                "[%s Execution Report] Branch: %s | Pass Rate: %s%%",
                                executionType, branchName, formattedPassRate);
            }

            StringBuilder buildDetails = new StringBuilder();
            buildDetails.append("<ul>");
            buildDetails
                    .append("    <li><strong>Branch:</strong> ")
                    .append(branchName)
                    .append("</li>");

            if (!"N/A".equalsIgnoreCase(imageName)) {
                buildDetails
                        .append("    <li><strong>Repository (Image Name):</strong> ")
                        .append(imageName)
                        .append("</li>");
            }
            if (!"N/A".equalsIgnoreCase(imageTag)) {
                buildDetails
                        .append("    <li><strong>Image Tag:</strong> ")
                        .append(imageTag)
                        .append("</li>");
            }
            buildDetails.append("</ul>");

            String headerTitle =
                    "BST".equalsIgnoreCase(executionType)
                            ? "BST Run Completion Report in QAF"
                            : String.format("%s Run Completion Report", executionType);

            String htmlBody =
                    String.format("<h2>%s</h2>", headerTitle)
                            + "<p>Hi Team,</p>"
                            + String.format(
                                    "<p>The %s execution has completed for the following build. Below is the summary of the test results.</p>",
                                    executionType)
                            + "<h3>Build Details</h3>"
                            + buildDetails.toString()
                            + "<h3>Test Execution Summary</h3>"
                            + "<table border=\"1\" cellpadding=\"5\" cellspacing=\"0\" style=\"border-collapse: collapse;\">"
                            + "    <tr style=\"background-color: #f2f2f2;\">"
                            + "        <th>Metric</th>"
                            + "        <th>Count</th>"
                            + "    </tr>"
                            + "    <tr>"
                            + "        <td><strong>Total Tests</strong></td>"
                            + "        <td>"
                            + total
                            + "</td>"
                            + "    </tr>"
                            + "    <tr>"
                            + "        <td style=\"color: green;\"><strong>Passed</strong></td>"
                            + "        <td style=\"color: green;\">"
                            + passed
                            + "</td>"
                            + "    </tr>"
                            + "    <tr>"
                            + "        <td style=\"color: red;\"><strong>Failed</strong></td>"
                            + "        <td style=\"color: red;\">"
                            + failed
                            + "</td>"
                            + "    </tr>"
                            + "    <tr>"
                            + "        <td style=\"color: orange;\"><strong>Skipped</strong></td>"
                            + "        <td style=\"color: orange;\">"
                            + skipped
                            + "</td>"
                            + "    </tr>"
                            + "    <tr>"
                            + "        <td><strong>Pass Rate</strong></td>"
                            + "        <td><strong>"
                            + formattedPassRate
                            + "%</strong></td>"
                            + "    </tr>"
                            + "</table>"
                            + "<br>"
                            + buildFailuresTable(failedItems, maxFailures, maxFailuresProp)
                            + "<p><a href=\""
                            + rpLink
                            + "\">Click here to view the detailed report on ReportPortal</a></p>"
                            + "<hr>"
                            + "<p><small>This is an automated notification; please do not reply directly to this email. If you have any questions, reach out to Kiran Gopi (KGopi@dbvi.com) or Jignesh Chhatbar (Jignesh.Chhatbar@dbvi.com).</small></p>";

            // 3. Send Email
            if (recipients == null || recipients.trim().isEmpty()) {
                ConsoleLogger.logData(
                        "Email recipients list is empty. Skipping email notification.");
            } else {
                sendEmail(recipients, ccRecipients, subject, htmlBody);
            }

        } catch (Exception e) {
            ConsoleLogger.logData("Error in ReportPortalEmailNotifier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isNumericOrUuid(String str) {
        if (str == null) return false;
        // Check if numeric
        if (str.matches("\\d+")) return true;
        // Check if UUID
        if (str.matches(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
            return true;
        return false;
    }

    private static int getIntFromPath(Response response, String path) {
        try {
            Object value = response.jsonPath().get(path);
            if (value == null) return 0;
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static class FailedItemDetail {
        final String scenarioName;
        final String stepName;
        final String errorMessage;

        FailedItemDetail(String scenarioName, String stepName, String errorMessage) {
            this.scenarioName = scenarioName;
            this.stepName = stepName;
            this.errorMessage = errorMessage;
        }
    }

    private static List<FailedItemDetail> fetchFailedItems(
            String rpEndpoint,
            String rpProjectId,
            String rpUuid,
            String rpLaunchId,
            int maxFailures) {

        List<FailedItemDetail> results = new ArrayList<>();

        // 1. Fetch the failed scenarios (hasStats=true)
        String itemUrl = rpEndpoint + "/api/v1/" + rpProjectId + "/item";

        Response itemResponse =
                RestAssured.given()
                        .header("Authorization", "Bearer " + rpUuid)
                        .contentType("application/json")
                        .queryParam("filter.eq.launchId", rpLaunchId)
                        .queryParam("filter.eq.hasStats", "true")
                        .queryParam("filter.eq.status", "FAILED")
                        .queryParam("filter.in.type", "STEP")
                        .queryParam("page.size", maxFailures)
                        .get(itemUrl);

        if (itemResponse.statusCode() != 200) {
            ConsoleLogger.logData(
                    "Failed to fetch failed scenarios. Status: " + itemResponse.statusCode());
            return results;
        }

        List<Map<String, Object>> scenarios = itemResponse.jsonPath().getList("content");
        if (scenarios == null || scenarios.isEmpty()) {
            ConsoleLogger.logData("No failed scenarios found.");
            return results;
        }

        ConsoleLogger.logData(
                "Processing " + scenarios.size() + " failed scenario(s) to find failed steps...");

        for (Map<String, Object> scenario : scenarios) {
            Object scenarioId = scenario.get("id");
            String scenarioName = String.valueOf(scenario.getOrDefault("name", "Unknown Scenario"));

            String failedStepName = scenarioName; // Fallback
            String errorMessage = "";

            if (scenarioId != null) {
                // 2. Fetch the actual failed step (hasStats=false) for this scenario
                Response stepResponse =
                        RestAssured.given()
                                .header("Authorization", "Bearer " + rpUuid)
                                .contentType("application/json")
                                .queryParam("filter.eq.launchId", rpLaunchId)
                                .queryParam("filter.eq.parentId", scenarioId)
                                .queryParam("filter.eq.status", "FAILED")
                                .get(itemUrl);

                if (stepResponse.statusCode() == 200) {
                    List<Map<String, Object>> steps = stepResponse.jsonPath().getList("content");
                    if (steps != null && !steps.isEmpty()) {
                        Map<String, Object> step = steps.get(0);
                        failedStepName = String.valueOf(step.get("name"));
                        Object stepId = step.get("id");

                        // 3. Fetch logs for this step using the verified pattern
                        String logUrl = rpEndpoint + "/api/v1/" + rpProjectId + "/log";
                        Response logResponse =
                                RestAssured.given()
                                        .header("Authorization", "Bearer " + rpUuid)
                                        .contentType("application/json")
                                        .queryParam("filter.eq.item", stepId)
                                        .queryParam("filter.gte.level", "TRACE")
                                        .queryParam("page.sort", "logTime,DESC")
                                        .queryParam("page.size", 10)
                                        .get(logUrl);

                        if (logResponse.statusCode() == 200) {
                            List<Map<String, Object>> logs =
                                    logResponse.jsonPath().getList("content");
                            if (logs != null && !logs.isEmpty()) {
                                // Try to find the first ERROR log in the most recent logs
                                Map<String, Object> errorLog = null;
                                for (Map<String, Object> log : logs) {
                                    String level = String.valueOf(log.get("level"));
                                    if ("ERROR".equalsIgnoreCase(level)
                                            || "FATAL".equalsIgnoreCase(level)) {
                                        errorLog = log;
                                        break;
                                    }
                                }

                                // If no ERROR log found, just take the very last log message
                                if (errorLog == null) {
                                    errorLog = logs.get(0);
                                }

                                String message = String.valueOf(errorLog.get("message"));
                                errorMessage =
                                        message.length() > 500
                                                ? message.substring(0, 500) + "..."
                                                : message;
                            }
                        }
                    }
                }
            }

            results.add(new FailedItemDetail("", failedStepName, errorMessage));
        }

        return results;
    }

    private static String buildFailuresTable(
            List<FailedItemDetail> failedItems, int maxFailures, String maxFailuresProp) {
        if (failedItems.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<h3 style=\"color: #cc0000;\">Failed Steps Detail</h3>");

        if (!"all".equalsIgnoreCase(maxFailuresProp) && failedItems.size() == maxFailures) {
            sb.append("<p><em>Showing top ")
                    .append(maxFailures)
                    .append(" failure(s). See ReportPortal for the full list.</em></p>");
        }

        sb.append(
                "<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\""
                        + " style=\"border-collapse: collapse; width: 100%; font-size: 13px;\">");
        sb.append("<tr style=\"background-color: #f2f2f2;\">");
        sb.append("<th style=\"width: 30px;\">#</th>");
        sb.append("<th>Failed Step</th>");
        sb.append("<th>Error Message</th>");
        sb.append("</tr>");

        for (int i = 0; i < failedItems.size(); i++) {
            FailedItemDetail detail = failedItems.get(i);
            String rowBg = (i % 2 == 0) ? "#fff5f5" : "#ffffff";
            sb.append("<tr style=\"background-color: ").append(rowBg).append(";\">");
            sb.append("<td style=\"text-align:center;\">").append(i + 1).append("</td>");
            sb.append("<td style=\"color: #8B0000; font-weight: bold;\">")
                    .append(escapeHtml(detail.stepName))
                    .append("</td>");
            sb.append(
                            "<td style=\"font-family: monospace; font-size: 11px; color: #cc0000;"
                                    + " white-space: pre-wrap; word-break: break-word;\">")
                    .append(escapeHtml(detail.errorMessage))
                    .append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table><br>");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void sendEmail(String to, String cc, String subject, String body) {
        String host =
                System.getProperty("mail.smtp.host", "mail.dbvi.com"); // Default Dbvi mail server
        String port = System.getProperty("mail.smtp.port", "25");
        String from = System.getProperty("mail.from", "ecomqa@dbvi.com");

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", port);

        Session session = Session.getDefaultInstance(properties);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            String[] recipientList = to.split(",");
            for (String recipient : recipientList) {
                message.addRecipient(
                        Message.RecipientType.TO, new InternetAddress(recipient.trim()));
            }

            if (cc != null && !cc.trim().isEmpty()) {
                String[] ccList = cc.split(",");
                for (String ccRecipient : ccList) {
                    message.addRecipient(
                            Message.RecipientType.CC, new InternetAddress(ccRecipient.trim()));
                }
            }

            message.setSubject(subject);
            message.setContent(body, "text/html");

            ConsoleLogger.logData("Sending email to: " + to);
            if (cc != null && !cc.trim().isEmpty()) {
                ConsoleLogger.logData("CC to: " + cc);
            }
            Transport.send(message);
            ConsoleLogger.logData("Email sent successfully.");

        } catch (MessagingException e) {
            ConsoleLogger.logData("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
