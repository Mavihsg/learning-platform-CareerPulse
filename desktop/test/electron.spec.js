const { _electron: electron, expect, test } = require('@playwright/test');
const path = require('path');

test.describe('CareerPulse Electron Desktop Application - End-to-End Suite', () => {
    let electronApp;
    let window;

    test.beforeAll(async () => {
        // Launch real Electron desktop application via Playwright's native _electron API
        electronApp = await electron.launch({
            args: [path.join(__dirname, '..')],
            env: {
                ...process.env,
                APP_URL: 'http://localhost:8080'
            }
        });

        // Wait for primary BrowserWindow
        window = await electronApp.firstWindow();
        await window.waitForLoadState('domcontentloaded');
    });

    test.afterAll(async () => {
        if (electronApp) {
            await electronApp.close();
        }
    });

    test('1. Validates native Electron window title and branding', async () => {
        const title = await window.title();
        expect(title).toContain('CareerPulse');
    });

    test('2. Ensures desktop mode layout is locked and mobile bottom nav is suppressed', async () => {
        // Verify document body has .is-electron-desktop class injected
        const hasDesktopClass = await window.evaluate(() => document.body.classList.contains('is-electron-desktop'));
        expect(hasDesktopClass).toBe(true);

        // Verify mobile bottom nav dock is completely hidden even if viewport was compressed
        const isBottomNavHidden = await window.evaluate(() => {
            const bnav = document.querySelector('.mobile-bottom-nav');
            if (!bnav) return true;
            const style = window.getComputedStyle(bnav);
            return style.display === 'none';
        });
        expect(isBottomNavHidden).toBe(true);

        // Verify mobile hamburger button is suppressed in desktop
        const isMobileToggleHidden = await window.evaluate(() => {
            const toggle = document.getElementById('btn-mobile-menu');
            if (!toggle) return true;
            const style = window.getComputedStyle(toggle);
            return style.display === 'none';
        });
        expect(isMobileToggleHidden).toBe(true);
    });

    test('3. Confirms secure contextBridge exposure of electronAPI', async () => {
        const apiCheck = await window.evaluate(() => {
            return {
                exists: typeof window.electronAPI !== 'undefined',
                hasPlatform: window.electronAPI && typeof window.electronAPI.platform !== 'undefined',
                hasVersion: window.electronAPI && typeof window.electronAPI.appVersion !== 'undefined'
            };
        });

        expect(apiCheck.exists).toBe(true);
        expect(apiCheck.hasPlatform).toBe(true);
    });

    test('4. Verifies Credential Verification modal functions inside Electron', async () => {
        await window.evaluate(() => {
            if (window.App && App.openPublicVerification) {
                App.openPublicVerification('CP-CERT-2026-10001');
            }
        });

        await window.waitForSelector('#credential-verification-modal', { state: 'visible', timeout: 5000 });
        const modalVisible = await window.isVisible('#credential-verification-modal');
        expect(modalVisible).toBe(true);
    });
});
