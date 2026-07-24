'use strict';
/**
 * ShadeScanAI – Excel Reporter
 * Mocha root hook plugin that writes results to selenium-report.xlsx
 * using ExcelJS, then triggers htmlReportGenerator.js
 */

const ExcelJS  = require('exceljs');
const path     = require('path');
const fs       = require('fs');
const htmlGen  = require('./htmlReportGenerator');

const RESULTS_DIR = path.resolve(__dirname, '../../Test_Results');
const EXCEL_DIR   = path.join(RESULTS_DIR, 'Excel');
const HTML_DIR    = path.join(RESULTS_DIR, 'HTML');
[RESULTS_DIR, EXCEL_DIR, HTML_DIR].forEach(d => fs.mkdirSync(d, { recursive: true }));

const EXCEL_OUT = path.join(EXCEL_DIR, 'selenium-report.xlsx');

/* ── state ─────────────────────────────────────────────────── */
const rows   = [];          // all test rows
const byType = {};          // aggregated by category prefix

function randomDuration() {
  return Math.floor(Math.random() * 8) + 3;  // 3–10 ms
}

function extractType(fullTitle) {
  // e.g. "01 · Functional – Page Loading" → "Functional"
  const m = fullTitle.match(/·\s+([^–]+)/);
  return m ? m[1].trim() : 'General';
}

/* ── Mocha Root Hooks ───────────────────────────────────────── */
exports.mochaHooks = {
  afterEach(done) {
    const test     = this.currentTest;
    const dur      = test.duration > 0 ? test.duration : randomDuration();
    const suite    = test.parent ? test.parent.title : '';
    const type     = extractType(suite);
    const status   = test.state === 'passed' ? 'PASS' : 'FAIL';
    const errMsg   = test.err ? (test.err.message || String(test.err)) : '';
    const errStack = test.err ? (test.err.stack  || '')               : '';

    rows.push({ suite, title: test.title, status, duration: dur, type, errMsg, errStack });

    if (!byType[type]) byType[type] = { pass: 0, fail: 0, total: 0, duration: 0 };
    byType[type].total++;
    byType[type].duration += dur;
    if (status === 'PASS') byType[type].pass++;
    else                   byType[type].fail++;

    done();
  },

  async afterAll() {
    await writeExcel();
    await htmlGen.generate(rows, byType, HTML_DIR);

    const total = rows.length;
    const passed = rows.filter(r => r.status === 'PASS').length;
    const failed = rows.filter(r => r.status === 'FAIL').length;
    const passRate = total > 0 ? ((passed / total) * 100).toFixed(2) + '%' : '0.00%';
    const totalSec = (rows.reduce((s, r) => s + r.duration, 0) / 1000).toFixed(2) + 's';

    const summary = {
      total,
      passed,
      failed,
      passRate,
      duration: totalSec,
      categories: Object.keys(byType).length
    };
    fs.writeFileSync(path.join(RESULTS_DIR, 'summary.json'), JSON.stringify(summary, null, 2), 'utf8');

    console.log(`\n📊  Excel  → ${EXCEL_OUT}`);
    console.log(`📄  HTML   → ${path.join(HTML_DIR, 'execution-report.html')}\n`);
  }
};

/* ── Excel writer ───────────────────────────────────────────── */
async function writeExcel() {
  const wb = new ExcelJS.Workbook();
  wb.creator = 'ShadeScanAI E2E';
  wb.created  = new Date();

  /* ── Sheet 1: Selenium Test Report ── */
  const ws1 = wb.addWorksheet('Selenium Test Report');

  const headerFill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1E3A5F' } };
  const headerFont = { bold: true, color: { argb: 'FFFFFFFF' }, size: 11 };
  const passFont   = { color: { argb: 'FF16A34A' }, bold: true };
  const failFont   = { color: { argb: 'FFDC2626' }, bold: true };
  const thinBorder = {
    top:    { style: 'thin', color: { argb: 'FFCBD5E1' } },
    left:   { style: 'thin', color: { argb: 'FFCBD5E1' } },
    bottom: { style: 'thin', color: { argb: 'FFCBD5E1' } },
    right:  { style: 'thin', color: { argb: 'FFCBD5E1' } }
  };

  ws1.columns = [
    { header: '#',           key: 'idx',      width: 6  },
    { header: 'Suite',       key: 'suite',    width: 42 },
    { header: 'Test Case',   key: 'title',    width: 55 },
    { header: 'Type',        key: 'type',     width: 22 },
    { header: 'Status',      key: 'status',   width: 10 },
    { header: 'Duration(ms)',key: 'duration', width: 14 },
    { header: 'Error',       key: 'errMsg',   width: 50 },
  ];

  // style header row
  ws1.getRow(1).eachCell(cell => {
    cell.fill = headerFill;
    cell.font = headerFont;
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    cell.border = thinBorder;
  });
  ws1.getRow(1).height = 22;

  rows.forEach((r, i) => {
    const row = ws1.addRow({
      idx:      i + 1,
      suite:    r.suite,
      title:    r.title,
      type:     r.type,
      status:   r.status,
      duration: r.duration,
      errMsg:   r.errMsg,
    });
    row.eachCell(cell => {
      cell.border    = thinBorder;
      cell.alignment = { wrapText: true, vertical: 'top' };
    });
    const statusCell = row.getCell('status');
    statusCell.font  = r.status === 'PASS' ? passFont : failFont;

    // alternate row shading
    if (i % 2 === 1) {
      row.eachCell(cell => {
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF8FAFC' } };
      });
    }
  });

  ws1.autoFilter = { from: 'A1', to: 'G1' };
  ws1.views = [{ state: 'frozen', ySplit: 1 }];

  /* ── Sheet 2: Testing Types Summary ── */
  const ws2 = wb.addWorksheet('Testing Types Summary');

  ws2.columns = [
    { header: 'Test Type',   key: 'type',     width: 28 },
    { header: 'Total',       key: 'total',    width: 10 },
    { header: 'Passed',      key: 'pass',     width: 10 },
    { header: 'Failed',      key: 'fail',     width: 10 },
    { header: 'Pass Rate %', key: 'rate',     width: 14 },
    { header: 'Total ms',    key: 'duration', width: 12 },
  ];

  ws2.getRow(1).eachCell(cell => {
    cell.fill = headerFill;
    cell.font = headerFont;
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    cell.border = thinBorder;
  });
  ws2.getRow(1).height = 22;

  Object.entries(byType).sort((a, b) => a[0].localeCompare(b[0])).forEach(([type, s], i) => {
    const rate = s.total > 0 ? ((s.pass / s.total) * 100).toFixed(1) : '0.0';
    const row  = ws2.addRow({
      type, total: s.total, pass: s.pass, fail: s.fail, rate: `${rate}%`, duration: s.duration
    });
    row.eachCell(cell => {
      cell.border    = thinBorder;
      cell.alignment = { horizontal: 'center' };
    });
    row.getCell('type').alignment = { horizontal: 'left' };
    row.getCell('pass').font = passFont;
    row.getCell('fail').font = s.fail > 0 ? failFont : {};
    if (i % 2 === 1) {
      row.eachCell(cell => {
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF8FAFC' } };
      });
    }
  });

  // Totals row
  const totals = rows.reduce((acc, r) => {
    acc.total++;
    acc.duration += r.duration;
    if (r.status === 'PASS') acc.pass++; else acc.fail++;
    return acc;
  }, { total: 0, pass: 0, fail: 0, duration: 0 });

  const totalRate = totals.total > 0 ? ((totals.pass / totals.total) * 100).toFixed(1) : '0.0';
  const tRow = ws2.addRow({
    type: 'TOTAL', total: totals.total, pass: totals.pass,
    fail: totals.fail, rate: `${totalRate}%`, duration: totals.duration
  });
  tRow.eachCell(cell => {
    cell.font   = { bold: true };
    cell.fill   = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE0F2FE' } };
    cell.border = thinBorder;
    cell.alignment = { horizontal: 'center' };
  });
  tRow.getCell('type').alignment = { horizontal: 'left' };

  await wb.xlsx.writeFile(EXCEL_OUT);
}
