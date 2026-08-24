package com.dbvi.automation.framework.utils.testdata;

import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.utils.TestData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserCredentialsProvider dynamically resolves user credentials (username, password, 2FA secret)
 * either from the PostgreSQL userdetails database table (with dynamic thread-safe locking) or falls
 * back to local environment testdata.yaml depending on config settings. All connection details are
 * resolved dynamically from FrameworkProperties.
 */
public class UserCredentialsProvider {
    private static final Logger logger = LoggerFactory.getLogger(UserCredentialsProvider.class);

    public static class UserCredentials {
        private final String username;
        private final String password;
        private final String totpSecret;

        public UserCredentials(String username, String password, String totpSecret) {
            this.username = username;
            this.password = password;
            this.totpSecret = totpSecret;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getTotpSecret() {
            return totpSecret;
        }
    }

    /**
     * Dynamically resolves user credentials (username, password, 2FA secret) either from the
     * PostgreSQL userdetails database table (with dynamic locking) or falls back to local
     * environment testdata.yaml depending on config settings.
     *
     * @param userType The Gherkin userType key (e.g. "regularuser", "rewarduser").
     * @return A valid UserCredentials object.
     */
    public static synchronized UserCredentials getUserCredentials(String userType) {
        if (userType.trim().equalsIgnoreCase("invalidUser")
                || userType.trim().equalsIgnoreCase("invalid_user")) {
            return new UserCredentials("invaliduser_9102788@dbvi.com", "InvalidPassword@123", "");
        }

        boolean readFromDb = FrameworkProperties.isReadCredentialsFromDb();
        String env = FrameworkProperties.getEnvironment();

        // Fetch 2FA secret from the local yaml file (always available locally)
        String totpSecret =
                TestData.get("users." + userType.toLowerCase() + ".twoFactorSecret", String.class);

        if (readFromDb) {
            logger.info(
                    "Database user credentials loading enabled. Fetching unlocked credentials for type: "
                            + userType);
            try {
                Class.forName("org.postgresql.Driver");
                Connection conn =
                        DriverManager.getConnection(
                                FrameworkProperties.getDbUrl(),
                                FrameworkProperties.getDbUsername(),
                                FrameworkProperties.getDbPassword());
                Statement stmt = conn.createStatement();

                // Query to select an unlocked user matching userType and environment
                String query =
                        String.format(
                                "SELECT username, password FROM \"EXECUTION\".userdetails "
                                        + "WHERE LOWER(usertype) = LOWER('%s') AND environment IN ('%s', 'ALL-QA') "
                                        + "AND lockedstatus = 'N' LIMIT 1",
                                userType, env);

                try (ResultSet rs = stmt.executeQuery(query)) {
                    if (rs.next()) {
                        String username = rs.getString("username");
                        String password = rs.getString("password");

                        // Dynamically LOCK the user inside the database to prevent other parallel
                        // threads from clashing!
                        String lockQuery =
                                String.format(
                                        "UPDATE \"EXECUTION\".userdetails SET lockedstatus = 'Y', lockedby = '%s' "
                                                + "WHERE username = '%s' AND LOWER(usertype) = LOWER('%s')",
                                        java.net.InetAddress.getLocalHost().getHostName(),
                                        username,
                                        userType);
                        stmt.executeUpdate(lockQuery);
                        logger.info(
                                "Successfully fetched and LOCKED user: "
                                        + username
                                        + " inside the database.");

                        return new UserCredentials(username, password, totpSecret);
                    }
                } finally {
                    stmt.close();
                    conn.close();
                }
            } catch (Exception e) {
                logger.error(
                        "Failed to fetch/lock user credentials from database: "
                                + e.getMessage()
                                + ". Falling back to local YAML.");
            }
        }

        // Fallback: load everything straight from the environment testdata.yaml
        logger.info("Loading user credentials from local YAML for type: " + userType);
        String username =
                TestData.get("users." + userType.toLowerCase() + ".username", String.class);
        String password =
                TestData.get("users." + userType.toLowerCase() + ".password", String.class);
        return new UserCredentials(username, password, totpSecret);
    }

    /**
     * Unlocks the given user inside the database at test teardown/completion.
     *
     * @param username The username of the user.
     * @param userType The userType key (e.g. "regularuser").
     */
    public static synchronized void unlockUser(String username, String userType) {
        if (!FrameworkProperties.isReadCredentialsFromDb()) {
            return;
        }
        logger.info("Unlocking database user: " + username);
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn =
                            DriverManager.getConnection(
                                    FrameworkProperties.getDbUrl(),
                                    FrameworkProperties.getDbUsername(),
                                    FrameworkProperties.getDbPassword());
                    Statement stmt = conn.createStatement()) {
                String unlockQuery =
                        String.format(
                                "UPDATE \"EXECUTION\".userdetails SET lockedstatus = 'N', lockedby = 'N' "
                                        + "WHERE username = '%s' AND LOWER(usertype) = LOWER('%s')",
                                username, userType);
                stmt.executeUpdate(unlockQuery);
                logger.info("Successfully UNLOCKED user: " + username + " inside the database.");
            }
        } catch (Exception e) {
            logger.error("Failed to unlock user in database: " + e.getMessage());
        }
    }
}
