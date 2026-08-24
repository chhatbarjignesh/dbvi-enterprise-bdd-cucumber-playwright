package com.dbvi.automation.pages;

import com.microsoft.playwright.Page;
import com.dbvi.automation.framework.factory.DriverFactory;
import com.dbvi.automation.framework.wrapper.ui.WebAction;

/**
 * BasePage is the foundation for all Page Objects in the framework. It dynamically retrieves the
 * thread-safe active Playwright Page instance on-the-fly, allowing parameterless, thread-safe
 * constructors across all page objects.
 */
public class BasePage {
    protected WebAction webAction = new WebAction();

    /** Retrieves the thread-safe active Playwright Page instance. */
    protected Page getPage() {
        Page page = DriverFactory.getPage();
        if (page == null) {
            throw new IllegalStateException(
                    "Playwright Page instance has not been initialized for this thread yet!");
        }
        return page;
    }

    /** Dynamic helper to retrieve the page title. */
    public String getTitle() {
        return getPage().title();
    }

    /**
     * Verifies that the page title contains the expected string. Performed inside the page class to
     * satisfy POM recommendations.
     */
    public void verifyPageTitleContains(String expectedTitle) {
        org.assertj.core.api.Assertions.assertThat(getTitle()).contains(expectedTitle);
    }

    /** Dynamic helper to retrieve the page URL. */
    public String getUrl() {
        return getPage().url();
    }

    /**
     * Centralized wait/delay helper. Pauses execution for the specified milliseconds. Encapsulates
     * Playwright's page.waitForTimeout natively.
     */
    public void sleep(long milliseconds) {
        try {
            getPage().waitForTimeout(milliseconds);
        } catch (Exception e) {
            // Safe ignore
        }
    }

    /**
     * Robustly waits for any active corporate loading spinners, dynamic overlays, or loaders to
     * completely disappear before attempting interactions.
     */
    public void waitForLoadingToDisappear() {
        try {
            com.microsoft.playwright.Locator loaders =
                    getPage()
                            .locator(
                                    ".LoaderCircular, .DynamicOverlayComponent, .Loader__child, div[class*='Loader']");
            // If any loader is visible, wait for it to be hidden or detached with a 10-second max
            // timeout
            if (loaders.first().isVisible()) {
                loaders.first()
                        .waitFor(
                                new com.microsoft.playwright.Locator.WaitForOptions()
                                        .setState(
                                                com.microsoft.playwright.options
                                                        .WaitForSelectorState.HIDDEN)
                                        .setTimeout(10000));
            }
        } catch (Exception e) {
            // Ignore timeout or missing element exceptions to keep flow seamless
        }
        // Small wait for DOM stabilization
        getPage().waitForTimeout(500);
    }
}
