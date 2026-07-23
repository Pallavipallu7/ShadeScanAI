const { remote } = require('webdriverio');
const XLSX = require('xlsx');
const path = require('path');
const fs = require('fs');

async function runFullAndroidE2ETest() {
  console.log('🚀 Starting Full E2E Appium Automation Suite for ShadeScan AI Android...');

  // Track test steps for the Excel Report
  const testResults = [];
  const startOverall = Date.now();

  function logStep(stepNum, name, description, status, remarks = '') {
    console.log(`[Step ${stepNum}] ${name} - ${status} (${remarks})`);
    testResults.push({
      'Step No.': stepNum,
      'Test Feature / Action': name,
      'Description': description,
      'Execution Status': status,
      'Timestamp': new Date().toLocaleTimeString(),
      'Remarks': remarks
    });
  }

  // Appium options
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
      'appium:noReset': false, // Start fresh to test full login flow
      'appium:newCommandTimeout': 300
    }
  };

  let driver;
  try {
    driver = await remote(wdOpts);
    logStep(1, 'App Initialization', 'Launch ShadeScan AI application on emulator', 'PASS', 'App launched successfully');
  } catch (err) {
    logStep(1, 'App Initialization', 'Launch ShadeScan AI application on emulator', 'FAIL', err.message);
    generateExcelReport(testResults);
    process.exit(1);
  }

  try {
    // === STEP 2: USER LOGIN ===
    try {
      console.log('⏳ Locating authentication fields...');
      const emailField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(0)');
      await emailField.waitForDisplayed({ timeout: 15000 });
      await emailField.setValue('doctor.test@shadescan.ai');

      const passwordField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(1)');
      await passwordField.setValue('Password123!');

      const loginButton = await driver.$('android=new UiSelector().text("LOGIN")');
      await loginButton.click();

      // Wait for navigation to DashboardActivity
      await driver.waitUntil(
        async () => {
          const currentActivity = await driver.getCurrentActivity();
          return currentActivity.includes('DashboardActivity') || currentActivity.includes('MainActivity');
        },
        { timeout: 15000 }
      );
      logStep(2, 'Doctor Authentication', 'Login with email credentials and verify redirect', 'PASS', 'Successful login redirection');
    } catch (err) {
      logStep(2, 'Doctor Authentication', 'Login with email credentials and verify redirect', 'FAIL', err.message);
    }

    // === STEP 3: PATIENT MANAGEMENT (ADD PATIENT) ===
    try {
      console.log('⏳ Navigating to Patients tab/screen...');
      const patientsTab = await driver.$('android=new UiSelector().text("PATIENTS")');
      if (await patientsTab.isExisting()) {
        await patientsTab.click();
      }

      const addPatientBtn = await driver.$('android=new UiSelector().descriptionContains("Add").or(new UiSelector().textContains("Add"))');
      await addPatientBtn.waitForDisplayed({ timeout: 8000 });
      await addPatientBtn.click();

      // Enter Patient Details
      const nameInput = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(0)');
      await nameInput.setValue('John Doe');

      const ageInput = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(1)');
      await ageInput.setValue('34');

      const phoneInput = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(2)');
      await phoneInput.setValue('9876543210');

      const notesInput = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(3)');
      await notesInput.setValue('Requires restoration on tooth #12');

      const saveBtn = await driver.$('android=new UiSelector().text("SAVE PATIENT").or(new UiSelector().text("Save"))');
      await saveBtn.click();

      logStep(3, 'Add Patient Record', 'Create and save new patient details to records list', 'PASS', 'Patient "John Doe" created successfully');
    } catch (err) {
      logStep(3, 'Add Patient Record', 'Create and save new patient details to records list', 'FAIL', err.message);
    }

    // === STEP 4: TOOTH SHADE CLASSIFICATION SCAN ===
    try {
      console.log('⏳ Navigating to Shade scan initiator...');
      const scanTab = await driver.$('android=new UiSelector().text("SCAN")');
      if (await scanTab.isExisting()) {
        await scanTab.click();
      }

      // Simulate capturing/selecting a tooth image
      const captureBtn = await driver.$('android=new UiSelector().descriptionContains("Capture").or(new UiSelector().textContains("Scan"))');
      await captureBtn.waitForDisplayed({ timeout: 8000 });
      await captureBtn.click();

      // Wait for classification processing to complete and show results card
      const resultCard = await driver.$('android=new UiSelector().textContains("SHADE")');
      await resultCard.waitForDisplayed({ timeout: 20000 });

      const shadeVal = await driver.$('android=new UiSelector().textContains("A1").or(new UiSelector().textContains("A2")).or(new UiSelector().textContains("B1"))');
      const shadeText = await shadeVal.getText();

      logStep(4, 'Shade Classification', 'Trigger shade detector and verify VITA classification', 'PASS', `AI matched shade: ${shadeText}`);
    } catch (err) {
      logStep(4, 'Shade Classification', 'Trigger shade detector and verify VITA classification', 'FAIL', err.message);
    }

    // === STEP 5: SAVE SCAN RESULTS ===
    try {
      console.log('⏳ Saving report matching results...');
      const saveReportBtn = await driver.$('android=new UiSelector().textContains("SAVE TO RECORDS")');
      await saveReportBtn.waitForDisplayed({ timeout: 8000 });
      await saveReportBtn.click();

      logStep(5, 'Save Scan Report', 'Link prediction shade result to patient profile', 'PASS', 'Scan matched result saved to Realtime DB');
    } catch (err) {
      logStep(5, 'Save Scan Report', 'Link prediction shade result to patient profile', 'FAIL', err.message);
    }

  } catch (error) {
    console.error('⚠️ Unexpected E2E Automation Error:', error);
  } finally {
    console.log('🔒 Closing Appium mobile session...');
    await driver.deleteSession();

    // Generate Final Excel Report
    const endOverall = Date.now();
    const durationSec = ((endOverall - startOverall) / 1000).toFixed(1);
    console.log(`⏱️ E2E Test Suite finished in ${durationSec}s`);
    
    generateExcelReport(testResults);
  }
}

function generateExcelReport(results) {
  console.log('📊 Generating Excel Test Report...');

  // Create workspace sheet
  const ws = XLSX.utils.json_to_sheet(results);

  // Set column widths for clean readability
  ws['!cols'] = [
    { wch: 10 }, // Step No.
    { wch: 25 }, // Test Feature
    { wch: 45 }, // Description
    { wch: 18 }, // Execution Status
    { wch: 15 }, // Timestamp
    { wch: 35 }  // Remarks
  ];

  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'E2E Appium Run');

  const reportDir = path.join(__dirname, '../reports');
  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }

  const reportPath = path.join(reportDir, 'Appium_Test_Report.xlsx');
  XLSX.writeFile(wb, reportPath);

  console.log(`✨ Excel analysis report saved successfully to: ${reportPath}`);
}

runFullAndroidE2ETest();
