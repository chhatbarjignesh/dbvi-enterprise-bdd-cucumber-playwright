package com.dbvi.automation.framework.perfecto;

import com.dbvi.automation.framework.config.FrameworkProperties;
import com.google.gson.Gson;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PerfectoReporter is a Playwright-native reporter helper class. It manages Perfecto Smart
 * Reporting entirely through browser-evaluated custom commands (page.evaluate()) without requiring
 * any external SDK jars.
 */
public class PerfectoReporter {
    private static final Logger logger = LoggerFactory.getLogger(PerfectoReporter.class);
    private final Page page;
    private final Gson gson = new Gson();
    private final String jobName;
    private final String projectName;
    private final String executionId;

    public PerfectoReporter(Page page) {
        this.page = page;
        this.jobName =
                getEnvOrProperty("PERFECTO_JOB_NAME", FrameworkProperties.getPerfectoJobName());
        this.projectName =
                getEnvOrProperty(
                        "PERFECTO_PROJECT_NAME", FrameworkProperties.getPerfectoProjectName());

        // Generate a unique client-side UUID for session linking
        this.executionId = java.util.UUID.randomUUID().toString();
    }

    private String getEnvOrProperty(String key, String defaultValue) {
        String val = System.getProperty(key);
        if (val == null) {
            val = System.getenv(key);
        }
        return val != null ? val : defaultValue;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getJobName() {
        return jobName;
    }

    /** Starts a logical test session in Perfecto Smart Reporting. */
    public void testStart(String scenarioName, List<String> tags) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", scenarioName);
            params.put("tags", tags);
            params.put("jobName", jobName);
            params.put("projectName", projectName);
            params.put("projectVersion", "1.0");
            params.put(
                    "executionId",
                    executionId); // Pass the client-generated UUID as the external ID

            String json = gson.toJson(params);
            page.evaluate("perfecto:report:testStart", json);
        } catch (Exception e) {
            logger.warn("Perfecto testStart reporting command failed: " + e.getMessage());
        }
    }

    /** Marks the start of a test step in Perfecto Smart Reporting. */
    public void stepStart(String stepName) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", stepName);

            String json = gson.toJson(params);
            page.evaluate("perfecto:report:stepStart", json);
        } catch (Exception e) {
            logger.warn("Perfecto stepStart reporting command failed: " + e.getMessage());
        }
    }

    /** Marks the completion of a test step in Perfecto Smart Reporting. */
    public void stepEnd(String message) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("message", message);

            String json = gson.toJson(params);
            page.evaluate("perfecto:report:stepEnd", json);
        } catch (Exception e) {
            logger.warn("Perfecto stepEnd reporting command failed: " + e.getMessage());
        }
    }

    /** Ends the logical test session and sets the final execution status. */
    public void testEnd(boolean success, String failureDescription) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("success", success);
            if (!success && failureDescription != null) {
                params.put("failureDescription", failureDescription);
            }

            String json = gson.toJson(params);
            page.evaluate("perfecto:report:testEnd", json);
        } catch (Exception e) {
            logger.warn("Perfecto testEnd reporting command failed: " + e.getMessage());
        }
    }

    /**
     * Queries Perfecto's Smart Reporting Public Export API after the session is closed to retrieve
     * the actual, finalized, and completely correct video report URL.
     */
    public static String getFinalizedPerfectoReportUrl(String jobName) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url =
                    "https://dbvi.app.perfectomobile.com/export/api/v3/test-executions?jobName[0]="
                            + URLEncoder.encode(jobName, StandardCharsets.UTF_8.name());

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "Perfecto-Authorization",
                                    FrameworkProperties.getPerfectoToken())
                            .GET()
                            .build();

            // Try up to 10 times (with 1 second delay) to allow Perfecto's reporting server to
            // index the completed run
            for (int i = 0; i < 10; i++) {
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String body = response.body();
                    int urlIndex = body.indexOf("\"reportURL\":\"");
                    if (urlIndex > 0) {
                        int startIndex = urlIndex + 13;
                        int endIndex = body.indexOf("\"", startIndex);
                        if (endIndex > startIndex) {
                            String reportUrl = body.substring(startIndex, endIndex);
                            // Clean up any unicode-encoded equal signs (\\u003d or \u003d) inside
                            // the JSON string
                            return reportUrl.replace("\\u003d", "=").replace("\u003d", "=");
                        }
                    }
                }
                Thread.sleep(1000); // Wait 1 second before retrying
            }
        } catch (Exception e) {
            // Ignore and fallback
        }
        return null;
    }
}
