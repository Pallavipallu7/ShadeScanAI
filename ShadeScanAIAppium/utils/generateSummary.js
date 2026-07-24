'use strict';
/**
 * ShadeScanAI Appium - GitHub Step Summary Generator
 * Reads .wdio-results.jsonl or test result objects and appends Markdown to GITHUB_STEP_SUMMARY.
 */

const fs = require('fs');
const path = require('path');

function generateStepSummary(results, byCategory) {
  const summaryFile = process.env.GITHUB_STEP_SUMMARY;
  if (!summaryFile) {
    console.log('ℹ️ GITHUB_STEP_SUMMARY environment variable not set (local execution).');
    return;
  }

  const total = results.length;
  const passed = results.filter(r => r.status === 'PASS' || r.state === 'passed').length;
  const failed = results.filter(r => r.status === 'FAIL' || r.state === 'failed').length;
  const passRate = total > 0 ? ((passed / total) * 100).toFixed(2) + '%' : '0.00%';
  const totalMs = results.reduce((acc, r) => acc + (r.duration || 0), 0);
  const durationSec = (totalMs / 1000).toFixed(2) + 's';

  const owner = (process.env.GITHUB_REPOSITORY_OWNER || 'pallavipallu7').toLowerCase();
  const repo = (process.env.GITHUB_REPOSITORY || 'Pallavipallu7/ShadeScanAI').split('/')[1] || 'ShadeScanAI';
  const runNumber = process.env.GITHUB_RUN_NUMBER || '1';

  const markdown = `
# 📱 Appium Mobile E2E Test Execution Summary (Build #${runNumber})

| Metric | Value | Status |
|---|---|---|
| **Total Tests** | ${total} | 📊 |
| **Passed** | ${passed} | 🟩 |
| **Failed** | ${failed} | ${failed === 0 ? '➖' : '❌'} |
| **Pass Rate** | ${passRate} | 🏆 |
| **Duration** | ${durationSec} | ⏱️ |

### 🌐 Mobile Test Reports & Deployment
| Item | Link / Details |
|------|----------------|
| 📄 **Live HTML Report (latest)** | [View Appium HTML Report](https://${owner}.github.io/${repo}/reports/latest/execution-report.html) |
| 📦 **Excel Artifact** | Actions → Artifacts → **appium-excel-report** |
| 📝 **HTML Artifact**  | Actions → Artifacts → **appium-html-report** |

> Executed on Android Emulator (API 29 Nexus 6) via Appium UIAutomator2 driver.
`;

  fs.appendFileSync(summaryFile, markdown, 'utf8');
  console.log('✅ Appium step summary written to $GITHUB_STEP_SUMMARY');
}

module.exports = { generateStepSummary };
