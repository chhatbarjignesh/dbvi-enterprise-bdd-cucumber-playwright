package com.dbvi.automation.steps;

import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.loggers.ReportLogger;
import com.dbvi.automation.framework.utils.TestData;
import com.dbvi.automation.framework.utils.TotpUtil;
import com.dbvi.automation.pages.example.HomePage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

public class ExampleSteps {

    private HomePage homePage = new HomePage();

    @Given("I navigate to {string}")
    public void i_navigate_to(String url) {
        homePage.navigate(url);
    }

    @Given("I navigate to the environment home page")
    public void i_navigate_to_the_environment_home_page() {
        String envUrl = FrameworkProperties.getAppUrl();
        homePage.navigate(envUrl);
    }

    @Given("I navigate to the mock home page")
    public void i_navigate_to_the_mock_home_page() {
        homePage.navigate("https://example.com");
    }

    @Given("I login as {string} user")
    public void i_login_as_user(String userType) {
        // Decoupled dynamic lookup: concatenates the userType string dynamically
        String username = TestData.get("users." + userType + ".username", String.class);
        String password = TestData.get("users." + userType + ".password", String.class);
        String totpSecret = TestData.get("users." + userType + ".twoFactorSecret", String.class);
        String currentTotp = TotpUtil.getTOTPCode(totpSecret);

        ReportLogger.logInfo(
                "Logging in dynamically: [userType="
                        + userType
                        + ", username="
                        + username
                        + ", password="
                        + password
                        + ", totp="
                        + currentTotp
                        + "]");

        Assertions.assertThat(username).isNotEmpty();
        Assertions.assertThat(password).isNotEmpty();
        Assertions.assertThat(currentTotp).hasSize(6);
    }

    @Then("I should see the text {string}")
    public void i_should_see_the_text(String text) {
        String bodyText = homePage.getBodyText();
        Assertions.assertThat(bodyText).contains(text);
    }

    @Then("I should verify the environment search properties")
    public void i_should_verify_the_environment_search_properties() {
        String term = TestData.get("search.term", String.class);
        Integer expectedResults = TestData.get("search.expectedResults", Integer.class);

        String regularUser = TestData.get("users.regularuser.username", String.class);
        String rewardUser = TestData.get("users.rewarduser.username", String.class);
        String diamondUser = TestData.get("users.diamonduser.username", String.class);

        // Fetch 2FA secret from the environment test data
        String regularSecret = TestData.get("users.regularuser.twoFactorSecret", String.class);
        // Generate a 6-digit TOTP code dynamically for 2FA verification
        String regularTotp = TotpUtil.getTOTPCode(regularSecret);

        ReportLogger.logInfo(
                "Dynamic Env Test Data Verified: [term="
                        + term
                        + ", expectedResults="
                        + expectedResults
                        + "]");
        ReportLogger.logInfo(
                "Fetched Multi-User Types: [regular="
                        + regularUser
                        + ", reward="
                        + rewardUser
                        + ", diamond="
                        + diamondUser
                        + "]");
        ReportLogger.logInfo(
                "Dynamically Generated 2FA TOTP Code for " + regularUser + " is: " + regularTotp);

        Assertions.assertThat(term).isNotEmpty();
        Assertions.assertThat(expectedResults).isPositive();
        Assertions.assertThat(regularUser).isNotEmpty();
        Assertions.assertThat(rewardUser).isNotEmpty();
        Assertions.assertThat(diamondUser).isNotEmpty();

        // Verify the dynamic 2FA code is exactly a 6-digit numeric string
        Assertions.assertThat(regularTotp).hasSize(6);
        Assertions.assertThat(regularTotp).containsOnlyDigits();

        // Verify global SSO and 2FA TOTP credentials from config properties (fully override-able
        // from Jenkins)
        String ssoUser = FrameworkProperties.getSsoUsername();
        String ssoPass = FrameworkProperties.getSsoPassword();
        String ssoSecret = FrameworkProperties.getSsoTotpSecret();
        String ssoTotp = TotpUtil.getTOTPCode(ssoSecret);

        ReportLogger.logInfo(
                "SSO 2FA TOTP Verified: [username="
                        + ssoUser
                        + ", password="
                        + ssoPass
                        + ", totpCode="
                        + ssoTotp
                        + "]");

        Assertions.assertThat(ssoUser).isNotEmpty();
        Assertions.assertThat(ssoPass).isNotEmpty();
        Assertions.assertThat(ssoTotp).hasSize(6);
        Assertions.assertThat(ssoTotp).containsOnlyDigits();
    }
}
