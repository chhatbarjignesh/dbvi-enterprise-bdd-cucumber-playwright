package com.dbvi.automation.pages.example;

import com.dbvi.automation.pages.BasePage;

/**
 * HomePage is the minimal reference Page Object backing the framework's own self-test (test.feature
 * / ExampleSteps). It demonstrates the Single-Class POM pattern: extend BasePage, drive
 * interactions through the inherited webAction wrapper, keep selectors as private constants.
 */
public class HomePage extends BasePage {

    private static final String BODY = "body";

    /** Navigates to the specified URL. */
    public HomePage navigate(String url) {
        webAction.goToURL(url);
        return this;
    }

    /** Retrieves the text of the entire page body. */
    public String getBodyText() {
        return webAction.getText(BODY);
    }
}
