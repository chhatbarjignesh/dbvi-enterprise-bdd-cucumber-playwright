package com.dbvi.automation.pages.dbvi;

import com.dbvi.automation.pages.BasePage;
import org.assertj.core.api.Assertions;

/**
 * Single-Class Page Object for the MMS Member Portal login flow. Covers the header "Log in" entry
 * point on the public home page and the hosted OIDC sign-in form served from
 * mmfauth.dadabhagwan.org.
 */
public class LoginPage extends BasePage {

    private static final String LOGIN_LINK = "text=Log in";
    private static final String EMAIL_INPUT = "#email";
    private static final String PASSWORD_INPUT = "#Password";
    private static final String LOGIN_BUTTON = "button[type='submit']";
    private static final String LOGGED_IN_USER_AVATAR = "span.symbol-label";

    /** Navigates to the specified URL. */
    public LoginPage navigate(String url) {
        webAction.goToURL(url);
        return this;
    }

    /** Clicks the "Log in" link in the home page header, redirecting to the hosted sign-in form. */
    public LoginPage clickLoginLink() {
        webAction.click(LOGIN_LINK);
        return this;
    }

    /** Enters credentials on the hosted sign-in form and submits the login. */
    public LoginPage login(String username, String password) {
        webAction.enterText(EMAIL_INPUT, username);
        webAction.enterSecureText(PASSWORD_INPUT, password);
        webAction.click(LOGIN_BUTTON);
        return this;
    }

    /** Verifies the member landed back on the portal with the logged-in avatar visible. */
    public void verifyLoginSuccessful() {
        Assertions.assertThat(webAction.isElementVisible(LOGGED_IN_USER_AVATAR, 15))
                .withFailMessage(
                        "Expected the logged-in member avatar to be visible after login, but it"
                                + " was not.")
                .isTrue();

        Assertions.assertThat(getUrl())
                .withFailMessage(
                        "Expected to be redirected away from the login page after a successful"
                                + " login, but URL was: "
                                + getUrl())
                .doesNotContain("/Account/Login");
    }
}
