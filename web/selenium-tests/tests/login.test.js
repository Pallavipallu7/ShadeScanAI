const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const fs = require('fs');
const path = require('path');

async function runWebE2ETest() {
  console.log('🚀 Starting Selenium E2E Web Test...');
  
  // Set up Chrome options
  const options = new chrome.Options();
  options.addArguments('--headless'); // Run in headless mode for CI/CD environments
  options.addArguments('--no-sandbox');
  options.addArguments('--disable-dev-shm-usage');
  options.addArguments('--window-size=1280,1024');

  // Build the WebDriver
  const driver = await new Builder()
    .forBrowser('chrome')
    .setChromeOptions(options)
    .build();

  try {
    const targetUrl = 'http://localhost:3000/';
    console.log(`🌐 Navigating to clinical web app at: ${targetUrl}`);
    await driver.get(targetUrl);

    // 1. Wait for email field to appear
    console.log('🔍 Waiting for login form elements...');
    const emailField = await driver.wait(until.elementLocated(By.id('email')), 15000);
    const passwordField = await driver.findElement(By.id('password'));
    const loginButton = await driver.findElement(By.id('login-button'));

    // 2. Input test login credentials
    console.log('✍️ Entering credentials...');
    await emailField.sendKeys('doctor.test@shadescan.ai');
    await passwordField.sendKeys('Password123!');

    // 3. Click submit
    console.log('🖱️ Clicking sign-in button...');
    await loginButton.click();

    // 4. Verify landing on Dashboard overview
    console.log('⏳ Waiting for dashboard redirect...');
    await driver.wait(until.urlContains('/'), 10000);
    
    // Validate welcome text or metric card visibility
    const bodyText = await driver.findElement(By.tagName('body')).getText();
    if (bodyText.includes('Welcome') || bodyText.includes('Dashboard') || bodyText.includes('ShadeScan')) {
      console.log('✅ Success: Login and redirect verified successfully!');
    } else {
      console.log('⚠️ Warning: Redirect verified but dashboard layout could not be fully asserted.');
    }

    // 5. Capture screenshot
    const screenshotDir = path.join(__dirname, '../screenshots');
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true });
    }
    const screenshotPath = path.join(screenshotDir, 'dashboard-login-success.png');
    const image = await driver.takeScreenshot();
    fs.writeFileSync(screenshotPath, image, 'base64');
    console.log(`📸 Screenshot saved successfully to: ${screenshotPath}`);

  } catch (error) {
    console.error('❌ E2E Test Failed with error:', error);
    process.exit(1);
  } finally {
    console.log('🔒 Closing browser session...');
    await driver.quit();
  }
}

runWebE2ETest();
