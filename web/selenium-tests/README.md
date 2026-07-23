# 🦷 ShadeScan AI — Web Selenium E2E Automation Testing Guide

This directory contains the Selenium WebDriver End-to-End (E2E) automation test suite for verifying the ShadeScan AI web portal login flow and dashboard landing.

---

## 🛠️ Prerequisites

Before running the tests, ensure you have the following installed on your machine:
1.  **Node.js** (v18 or higher recommended)
2.  **Google Chrome** browser
3.  **ChromeDriver** (matching your installed Chrome version)

---

## 🚀 Setup & Execution Instructions

### Step 1: Install Dependencies
Open your terminal inside the `web/selenium-tests/` directory and run:
```bash
npm install
```

### Step 2: Start the Web Application
Make sure your React web application is running locally. In the `web/` folder, run:
```bash
npm run dev
```
*(Verify the application is accessible at `http://localhost:3000`)*

### Step 3: Run the Selenium E2E Tests
To execute the login test case, run:
```bash
npm run test:login
```

---

## 📁 Test Case Breakdown

### `tests/login.test.js`
*   **Target Elements**: Automates fields with stable IDs: `#email`, `#password`, and `#login-button`.
*   **Flow**:
    1.  Launches a headless Chrome browser instance.
    2.  Navigates to `http://localhost:3000`.
    3.  Enters clinical test credentials.
    4.  Clicks the sign-in button.
    5.  Waits for redirect validation.
    6.  Saves a success screenshot to `screenshots/dashboard-login-success.png`.
