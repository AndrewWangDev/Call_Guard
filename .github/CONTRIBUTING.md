# Contributing to Compose Call Guard

Thank you for your interest in Compose Call Guard!

## Repository Status: Archived & Read-Only

This project has achieved its architectural goals and is currently in a **read-only / archived** status.

- **Pull Requests**: New feature pull requests are no longer being reviewed or merged into `main`.
- **Forks**: You are completely free and warmly encouraged to fork this repository under the terms of the [MIT License](../LICENSE) and maintain independent distributions.

## Local Development & Verification

If you are experimenting with a fork locally:
1. Requirements: Android Studio Iguana+, JDK 21, and Android SDK 34.
2. Run unit test suite:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. Build unsigned debug/release artifacts:
   ```bash
   ./gradlew assembleDebug assembleRelease
   ```
