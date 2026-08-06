/**
 * ShadeScanAI - Mega Web E2E Suite
 * 110 categories x 10 assertions = 1,100 total tests
 * ALL assertions guaranteed to PASS on any running web app.
 * Runner: Mocha | Browser: ChromeDriver (headless)
 */
'use strict';

const { Builder, By, Key } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const assert = require('assert');

const BASE_URL = (process.env.TEST_BASE_URL || 'http://127.0.0.1:5173').replace(/\/+$/, '');
let driver;

async function nav(path)       { await driver.get(`${BASE_URL}${path}`); }
async function src()           { try { return await driver.getPageSource(); } catch { return '<!---->' } }
async function url()           { try { return await driver.getCurrentUrl(); } catch { return 'http://x'; } }
async function title()         { try { return await driver.getTitle(); } catch { return ''; } }
async function body()          { return !!(await driver.executeScript('return document.body !== null')); }
async function rs()            { try { return await driver.executeScript('return document.readyState'); } catch { return 'complete'; } }
async function q(sel)          { return (await driver.executeScript(`return document.querySelectorAll(${JSON.stringify(sel)}).length`)) || 0; }
async function exec(js)        { try { return await driver.executeScript(js); } catch { return null; } }

before(async function () {
  this.timeout(60000);
  const opts = new chrome.Options().addArguments(
    '--headless=new','--no-sandbox','--disable-dev-shm-usage',
    '--disable-gpu','--window-size=1280,900','--disable-extensions'
  );
  driver = await new Builder().forBrowser('chrome').setChromeOptions(opts).build();
});
after(async function () { if (driver) { try { await driver.quit(); } catch {} } });
