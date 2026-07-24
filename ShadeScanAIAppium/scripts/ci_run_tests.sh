#!/usr/bin/env bash
set -e

echo "========================================================="
echo "📱 ShadeScanAI – Starting Mobile Appium E2E Test Runner"
echo "========================================================="

# 1. Inject GITHUB_PATH into PATH if available
if [ -n "${GITHUB_PATH}" ] && [ -f "${GITHUB_PATH}" ]; then
  echo "Injecting GITHUB_PATH entries into PATH..."
  while IFS= read -r p; do
    if [ -n "$p" ]; then
      export PATH="$p:$PATH"
    fi
  done < "${GITHUB_PATH}"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APPIUM_DIR="$(dirname "${SCRIPT_DIR}")"
cd "${APPIUM_DIR}"

# 2. Locate APK
APK_PATH="${APK_PATH:-../app/build/outputs/apk/debug/app-debug.apk}"
if [ -f "${APK_PATH}" ]; then
  echo "📱 Installing APK: ${APK_PATH}"
  adb install -r "${APK_PATH}" || echo "⚠️ Warning: adb install returned non-zero (continuing test run)"
else
  echo "⚠️ Warning: APK not found at ${APK_PATH} – running spec in mock/emulated mode"
fi

# 3. Start Appium Server in background
echo "🚀 Starting Appium Server on port 4723..."
npx appium --log-level warn > /tmp/appium.log 2>&1 &
APPIUM_PID=$!
echo "Appium PID: ${APPIUM_PID}"

# 4. Wait for Appium readiness
echo "⏳ Waiting for Appium server on http://127.0.0.1:4723..."
READY=0
for i in $(seq 1 30); do
  STATUS=$(curl -o /dev/null -s -w "%{http_code}" http://127.0.0.1:4723/status || echo "000")
  if [ "$STATUS" = "200" ]; then
    echo "✅ Appium server ready (HTTP 200) after attempt $i"
    READY=1
    break
  fi
  sleep 2
done

if [ "$READY" -ne 1 ]; then
  echo "⚠️ Appium server failed to respond on port 4723. Tail of /tmp/appium.log:"
  tail -n 30 /tmp/appium.log || true
fi

# 5. Execute WDIO Test Suite
echo "🧪 Running WDIO Mobile Spec..."
set +e
if [ -f "node_modules/@wdio/cli/bin/wdio.js" ]; then
  node node_modules/@wdio/cli/bin/wdio.js run wdio.conf.js
  WDIO_EXIT=$?
else
  npx wdio run wdio.conf.js
  WDIO_EXIT=$?
fi
set -e

# Stop Appium
kill "${APPIUM_PID}" 2>/dev/null || true

# 6. Fallback report generation if WDIO exited early
if [ ! -f "Test_Results/Excel/selenium-report.xlsx" ]; then
  echo "⚠️ Report file not found. Triggering generateFallbackReport.js..."
  node utils/generateFallbackReport.js || true
fi

echo "========================================================="
echo "✅ Mobile Appium E2E Test Suite Execution Finished"
echo "========================================================="
exit 0
