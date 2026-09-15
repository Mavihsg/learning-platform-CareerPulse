# CareerPulse Desktop Client (Electron)

This directory contains the cross-platform desktop application for **CareerPulse**, built with **Electron**. It provides a native desktop experience wrapping the responsive web application with native menus, system tray, window management, and offline resilience.

## Features
- **Native Experience**: Native OS window framing with dark theme aesthetics (`#090B10`), window state persistence, and native application menus.
- **Offline Resilience**: Automatically displays a sleek branded CareerPulse offline screen with countdown and retry if the backend server is unreachable.
- **External Links**: YouTube lesson embeds, documentation, and external links automatically launch in the user's default web browser.
- **Secure Sandboxing**: Context isolation enabled with zero Node.js exposure to remote web content.

## Quick Start (Development)

1. Ensure the CareerPulse backend is running:
   ```bash
   # From the project root
   mvn spring-boot:run
   ```

2. Install dependencies:
   ```bash
   cd desktop
   npm install
   ```

3. Launch the desktop application:
   ```bash
   npm start
   ```

To point to a remote deployment URL:
```bash
APP_URL=https://careerpulse.app npm start
```

## Packaging Installers

Package standalone desktop installers using `electron-builder`:

```bash
# Windows (.exe installer & portable)
npm run dist:win

# macOS (.dmg)
npm run dist:mac

# Linux (.AppImage, .deb)
npm run dist:linux
```
