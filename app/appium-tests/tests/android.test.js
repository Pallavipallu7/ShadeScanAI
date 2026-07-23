const { remote } = require('webdriverio');

async function runAndroidE2ETest() {
  console.log('🚀 Starting Appium (Selenium WebDriver) Android E2E Test...');

  const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    path: '/',
    capabilities: {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': 'Android Emulator',
      'appium:appPackage': 'com.example.dental_shade_app',
      'appium:appActivity': '.LoginActivity',
      'appium:noReset': true,
      'appium:newCommandTimeout': 240
    }
  };

  const driver = await remote(wdOpts);

  try {
    console.log('⏳ Waiting for Login Screen to load...');
    
    // 1. Locate email input field
    const emailField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(0)');
    await emailField.waitForDisplayed({ timeout: 15000 });
    console.log('✍️ Entering email address...');
    await emailField.setValue('doctor.test@shadescan.ai');

    // 2. Locate password input field
    const passwordField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(1)');
    console.log('✍️ Entering password...');
    await passwordField.setValue('Password123!');

    // 3. Locate and click Login Button
    const loginButton = await driver.$('android=new UiSelector().text("LOGIN")');
    console.log('🖱️ Clicking LOGIN button...');
    await loginButton.click();

    // 4. Verify landing on DashboardActivity
    console.log('⏳ Checking for Dashboard redirection...');
    await driver.waitUntil(
      async () => {
        const currentActivity = await driver.getCurrentActivity();
        return currentActivity.includes('DashboardActivity');
      },
      {
        timeout: 15000,
        timeoutMsg: 'DashboardActivity redirection timed out'
      }
    );

    console.log('✅ Success: Android App login and dashboard landing verified!');

  } catch (error) {
    console.error('❌ E2E Mobile Test Failed:', error);
    process.exit(1);
  } finally {
    console.log('🔒 Closing Appium session...');
    await driver.deleteSession();
  }
}

runAndroidE2ETest();
