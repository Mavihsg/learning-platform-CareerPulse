package com.learning.platform.ui;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BasePlaywrightUiTest {

    @LocalServerPort
    protected int port;

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    void initPlaywright() {
        Map<String, String> env = new HashMap<>();
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        Playwright.CreateOptions createOptions = new Playwright.CreateOptions().setEnv(env);
        playwright = Playwright.create(createOptions);

        boolean headless = Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));
        double slowMo = headless ? 0 : 500;
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo);

        // Resilient browser launch strategy:
        // Use pre-installed Microsoft Edge or Google Chrome on Windows (zero network download required)
        try {
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setSlowMo(slowMo)
                    .setChannel("msedge"));
        } catch (Exception e1) {
            try {
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo)
                        .setChannel("chrome"));
            } catch (Exception e2) {
                browser = playwright.chromium().launch(options);
            }
        }
    }

    @AfterAll
    void closePlaywright() {
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
        page = context.newPage();
    }

    @AfterEach
    void cleanupContext(TestInfo testInfo) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception ignored) {}
        }
    }

    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    protected void navigateHome() {
        page.navigate(getBaseUrl() + "/");
        dismissNetflixIntro();
    }

    protected void dismissNetflixIntro() {
        try {
            // Dismiss intro smoothly by triggering display none on overlay if present
            page.evaluate("() => { const o = document.getElementById('netflix-intro-overlay'); if (o) o.style.display = 'none'; }");
        } catch (Exception ignored) {}
    }

    protected void openAuthModal() {
        navigateHome();
        Locator authModal = page.locator("#auth-modal");
        if (!authModal.isVisible()) {
            Locator signinBtn = page.locator("#nav-signin-btn");
            if (signinBtn.count() > 0 && signinBtn.isVisible()) {
                signinBtn.click();
            } else {
                page.evaluate("() => { if (window.App && App.showAuthModal) App.showAuthModal(); }");
            }
        }
        page.waitForSelector("#auth-modal", new Page.WaitForSelectorOptions().setTimeout(5000));
    }

    protected void captureScreenshot(String testName) {
        try {
            new File("target/playwright-screenshots").mkdirs();
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("target/playwright-screenshots/" + testName + ".png")));
        } catch (Exception ignored) {}
    }
}
