# 📱 ShadeScan AI — Android Appium E2E Automation Testing Guide

This directory contains the Appium (Selenium WebDriver mobile protocol) End-to-End (E2E) automation test suite for verifying the ShadeScan AI native Android application login flow and dashboard redirection.

---

## 🛠️ Prerequisites

Before running the tests, ensure you have the following installed on your machine:
1.  **Node.js** (v18 or higher recommended)
2.  **Appium Server** (v2.x or higher)
3.  **Appium UiAutomator2 Driver** (for Android automation)
4.  **Android Studio** with a running **Android Emulator** or a connected physical Android device.
5.  **Java Development Kit (JDK 17 or JDK 21)** configured in your env variables.

---

## 🚀 Setup & Execution Instructions

### Step 1: Install Dependencies
Open your terminal inside the `app/appium-tests/` directory and run:
```bash
npm install
```

### Step 2: Install Appium Drivers (One-time setup)
If you do not have the Appium UiAutomator2 driver installed globally, run:
```bash
npm install -g appium
appium driver install uiautomator2
```

### Step 3: Build the Android App
Build the APK from the root project folder:
```bash
./gradlew assembleDebug
```
*(The generated APK will be at: `app/build/outputs/apk/debug/app-debug.apk`)*

### Step 4: Start Appium Server
Start the Appium server in a separate terminal:
```bash
appium
```
*(Verify it starts on default port `4723`)*

### Step 5: Start your Android Emulator / Connect Device
Make sure an Android Emulator is running and active (check via `adb devices`).

### Step 6: Run the Appium E2E Tests
To execute the automation test case, run:
```bash
npm run test:android
```

---

## 📁 Test Case Breakdown

### `tests/android.test.js`
*   **Target Package**: `com.example.dental_shade_app`
*   **Target Activity**: `.LoginActivity` (Redirection verified to `.DashboardActivity`)
*   **Flow**:
    1.  Establishes session with running Android Emulator via Appium Server.
    2.  Locates username/password inputs using Android UI selectors.
    3.  Inputs clinical test credentials.
    4.  Triggers the login validation flow.
    5.  Waits for redirection validation to the dashboard landing screen.
