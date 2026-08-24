package com.dbvi.automation.steps.dbvi;

import com.dbvi.automation.framework.utils.EncryptionUtil;
import com.dbvi.automation.framework.utils.TestData;
import com.dbvi.automation.pages.dbvi.LoginPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LoginSteps {

    private final LoginPage loginPage = new LoginPage();

    @When("I click on the login link")
    public void i_click_on_the_login_link() {
        loginPage.clickLoginLink();
    }

    @And("I login to MMS portal as {string} user")
    public void i_login_to_mms_portal_as_user(String userType) {
        String username = TestData.get("users." + userType + ".username", String.class);
        String encryptedPassword = TestData.get("users." + userType + ".password", String.class);
        String password;
        try {
            password = EncryptionUtil.decrypt(encryptedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password for user type: " + userType, e);
        }
        loginPage.login(username, password);
    }

    @Then("I should be logged in successfully to MMS")
    public void i_should_be_logged_in_successfully_to_mms() {
        loginPage.verifyLoginSuccessful();
    }
}
