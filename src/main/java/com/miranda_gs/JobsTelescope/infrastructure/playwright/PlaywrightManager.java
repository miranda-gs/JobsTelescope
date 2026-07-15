package com.miranda_gs.JobsTelescope.infrastructure.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

public class PlaywrightManager {

    private final Playwright playwright;
    private final Browser browser;

    public PlaywrightManager() {
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch();
    }

    public Page newPage(String url, int timeoutMillis) {
        var context = browser.newContext();
        var page = context.newPage();
        page.navigate(url, new Page.NavigateOptions()
                .setTimeout(timeoutMillis)
                .setWaitUntil(WaitUntilState.NETWORKIDLE));
        return page;
    }

    public String fetchHtml(String url, int timeoutMillis) {
        try (var page = newPage(url, timeoutMillis)) {
            return page.content();
        }
    }

    public void close() {
        browser.close();
        playwright.close();
    }
}