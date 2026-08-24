package com.dbvi.automation.framework.wrapper.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.dbvi.automation.framework.config.FrameworkProperties;
import com.dbvi.automation.framework.factory.DriverFactory;
import com.dbvi.automation.framework.loggers.ReportLogger;

/**
 * WebAction is a fluent wrapper for Playwright interactions. It automatically enforces
 * pre-interaction dynamic wait states on every action to completely eliminate UI flakiness.
 */
public class WebAction {

    private Page getPage() {
        return DriverFactory.getPage();
    }

    /**
     * Helper to wait dynamically for element visibility up to a custom timeout in seconds.
     *
     * @param selector The element locator selector.
     * @param timeoutInSeconds The maximum time to wait in seconds.
     */
    public void waitForElementToBeVisible(String selector, int timeoutInSeconds) {
        getPage()
                .locator(selector)
                .first()
                .waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(timeoutInSeconds * 1000));
    }

    /** Helper to wait dynamically for element visibility up to the configured global timeout. */
    public void waitForElementToBeVisible(String selector) {
        waitForElementToBeVisible(selector, FrameworkProperties.getPlaywrightTimeout());
    }

    /**
     * Helper to wait dynamically for element invisibility up to a custom timeout in seconds.
     *
     * @param selector The element locator selector.
     * @param timeoutInSeconds The maximum time to wait in seconds.
     */
    public void waitForElementToBeHidden(String selector, int timeoutInSeconds) {
        getPage()
                .locator(selector)
                .first()
                .waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.HIDDEN)
                                .setTimeout(timeoutInSeconds * 1000));
    }

    /** Helper to wait dynamically for element invisibility up to the configured global timeout. */
    public void waitForElementToBeHidden(String selector) {
        waitForElementToBeHidden(selector, FrameworkProperties.getPlaywrightTimeout());
    }

    public WebAction goToURL(String url) {
        ReportLogger.logInfo("Navigating to URL: " + url);
        getPage().navigate(url);
        return this;
    }

    public WebAction click(String selector) {
        ReportLogger.logInfo("Clicking on element: " + selector);
        waitForElementToBeVisible(selector);
        getPage().locator(selector).first().click();
        return this;
    }

    public WebAction clickWithForce(String selector) {
        ReportLogger.logInfo("Force clicking on element: " + selector);
        waitForElementToBeVisible(selector);
        getPage().locator(selector).first().click(new Locator.ClickOptions().setForce(true));
        return this;
    }

    public WebAction clickUsingJS(String selector) {
        ReportLogger.logInfo("Clicking on element using JavaScript: " + selector);
        try {
            if (selector.startsWith("//") || selector.startsWith("xpath=")) {
                String xpath = selector.replace("xpath=", "").replace("\"", "\\\"");
                getPage()
                        .evaluate(
                                "document.evaluate(\""
                                        + xpath
                                        + "\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click()");
            } else {
                String css = selector.replace("\"", "\\\"");
                getPage().evaluate("document.querySelector(\"" + css + "\").click()");
            }
        } catch (Exception e) {
            ReportLogger.logInfo(
                    "JavaScript click failed: "
                            + e.getMessage()
                            + ". Falling back to native click.");
            click(selector);
        }
        return this;
    }

    public WebAction doubleClickOnElement(String selector) {
        ReportLogger.logInfo("Double clicking on element: " + selector);
        waitForElementToBeVisible(selector);
        getPage().locator(selector).first().dblclick();
        return this;
    }

    public WebAction enterText(String selector, String textToEnter) {
        ReportLogger.logInfo("Entering text into element " + selector + ": " + textToEnter);
        waitForElementToBeVisible(selector);
        getPage().locator(selector).first().fill(textToEnter);
        return this;
    }

    /**
     * Same as {@link #enterText(String, String)}, but masks the value in logs. Use for passwords
     * and other sensitive fields that should never appear in console/Allure/ReportPortal output.
     */
    public WebAction enterSecureText(String selector, String textToEnter) {
        ReportLogger.logInfo("Entering text into element " + selector + ": ********");
        waitForElementToBeVisible(selector);
        getPage().locator(selector).first().fill(textToEnter);
        return this;
    }

    public WebAction pressKey(String selector, String key) {
        ReportLogger.logInfo("Pressing key '" + key + "' on element: " + selector);
        getPage().locator(selector).first().press(key);
        return this;
    }

    public WebAction selectFromDropDown(String selector, String textToSelect) {
        ReportLogger.logInfo(
                "Selecting option by label '" + textToSelect + "' from dropdown: " + selector);
        waitForElementToBeVisible(selector);
        getPage().locator(selector).first().selectOption(new SelectOption().setLabel(textToSelect));
        return this;
    }

    public WebAction selectFromDropDownByValue(String selector, String valueToSelect) {
        ReportLogger.logInfo(
                "Selecting option by value '" + valueToSelect + "' from dropdown: " + selector);
        waitForElementToBeVisible(selector);
        getPage()
                .locator(selector)
                .first()
                .selectOption(new SelectOption().setValue(valueToSelect));
        return this;
    }

    public WebAction scrollToAnElement(String selector) {
        ReportLogger.logInfo("Scrolling element into view: " + selector);
        getPage().locator(selector).first().scrollIntoViewIfNeeded();
        return this;
    }

    public WebAction acceptAlert() {
        ReportLogger.logInfo("Registering auto-accept dialog listener.");
        getPage()
                .onDialog(
                        dialog -> {
                            ReportLogger.logInfo(
                                    "Auto-accepting dialog: [type="
                                            + dialog.type()
                                            + ", message="
                                            + dialog.message()
                                            + "]");
                            dialog.accept();
                        });
        return this;
    }

    public WebAction mouseHover(String selector) {
        ReportLogger.logInfo("Hovering mouse over element: " + selector);
        waitForElementToBeVisible(selector);
        getPage().locator(selector).first().hover();
        return this;
    }

    public WebAction refreshCurrentPage() {
        ReportLogger.logInfo("Refreshing the current page.");
        getPage().reload();
        return this;
    }

    public WebAction navigateBack() {
        ReportLogger.logInfo("Navigating back to the previous page.");
        getPage().goBack();
        return this;
    }

    public String getText(String selector) {
        waitForElementToBeVisible(selector);
        String innerText = getPage().locator(selector).first().innerText();
        ReportLogger.logInfo(
                "Retrieved inner text from element " + selector + ": '" + innerText + "'");
        return innerText;
    }

    /**
     * Checks if an element is visible, waiting dynamically up to a specified timeout in seconds.
     *
     * @param selector The element locator selector.
     * @param timeoutInSeconds The maximum time to wait in seconds.
     * @return true if the element becomes visible within the timeout, false otherwise.
     */
    public boolean isElementVisible(String selector, int timeoutInSeconds) {
        try {
            getPage()
                    .locator(selector)
                    .first()
                    .waitFor(
                            new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.VISIBLE)
                                    .setTimeout(timeoutInSeconds * 1000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Checks if an element is visible, waiting dynamically up to a default of 5 seconds. */
    public boolean isElementVisible(String selector) {
        return isElementVisible(selector, 5);
    }
}
