'use strict';
/**
 * ShadeScanAI Appium - HTML Report Generator
 * Renders dark-themed execution-report.html with stats, charts, badges, and test case tables.
 */

const fs = require('fs');
const path = require('path');

function generateHtmlReport(results, byCategory, outDir) {
  fs.mkdirSync(outDir, { recursive: true });

  const total = results.length;
  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  const passRate = total > 0 ? ((passed / total) * 100).toFixed(1) : '0.0';
  const totalMs = results.reduce((s, r) => s + r.duration, 0);
  const genTime = new Date().toUTCString();

  const typeSummaryRows = Object.entries(byCategory)
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([cat, s]) => {
      const rate = s.total > 0 ? ((s.pass / s.total) * 100).toFixed(1) : '0.0';
      const bar = `<div class="bar-wrap"><div class="bar-fill" style="width:${rate}%"></div></div>`;
      return `<tr>
        <td>${escHtml(cat)}</td>
        <td>${s.total}</td>
        <td class="pass-text">${s.pass}</td>
        <td class="${s.fail > 0 ? 'fail-text' : ''}">${s.fail}</td>
        <td>${rate}% ${bar}</td>
        <td>${s.duration} ms</td>
      </tr>`;
    }).join('\n');

  const testRows = results.map((r, i) => {
    const badge = r.status === 'PASS'
      ? '<span class="badge pass-badge">PASS</span>'
      : '<span class="badge fail-badge">FAIL</span>';
    const errCell = r.errMsg
      ? `<details><summary class="err-summary">Error</summary><pre class="err-pre">${escHtml(r.errMsg)}</pre></details>`
      : '—';
    return `<tr class="${i % 2 === 0 ? 'row-even' : 'row-odd'}">
      <td class="idx-cell">${i + 1}</td>
      <td class="suite-cell">${escHtml(r.category)}</td>
      <td>${escHtml(r.title)}</td>
      <td><span class="type-chip">Mobile</span></td>
      <td>${badge}</td>
      <td>${r.duration} ms</td>
      <td>${errCell}</td>
    </tr>`;
  }).join('\n');

  const donut = buildDonut(passed, failed);
  const categoryChart = buildCategoryChart(byCategory);

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>ShadeScanAI – Appium Mobile E2E Report</title>
<style>
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Segoe UI',system-ui,sans-serif;background:#0f172a;color:#e2e8f0;font-size:14px}
  a{color:#38bdf8}
  h1{font-size:1.6rem;font-weight:700;color:#f1f5f9}
  h2{font-size:1.1rem;font-weight:600;color:#94a3b8;margin-bottom:.75rem}
  .header{background:linear-gradient(135deg,#1e3a5f 0%,#0f172a 100%);padding:2rem 2.5rem;border-bottom:1px solid #1e293b}
  .meta{color:#94a3b8;font-size:.82rem;margin-top:.4rem}
  .container{max-width:1400px;margin:0 auto;padding:2rem 2.5rem}
  .cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:1rem;margin-bottom:2rem}
  .card{background:#1e293b;border:1px solid #334155;border-radius:12px;padding:1.25rem 1.5rem}
  .card-label{font-size:.75rem;color:#64748b;text-transform:uppercase;letter-spacing:.06em;margin-bottom:.4rem}
  .card-value{font-size:2rem;font-weight:700}
  .card-value.green{color:#22c55e} .card-value.red{color:#ef4444}
  .card-value.blue{color:#38bdf8}  .card-value.amber{color:#f59e0b}
  .charts-row{display:grid;grid-template-columns:260px 1fr;gap:1.5rem;margin-bottom:2rem;align-items:start}
  .chart-box{background:#1e293b;border:1px solid #334155;border-radius:12px;padding:1.5rem}
  .donut-wrap{display:flex;flex-direction:column;align-items:center;gap:.75rem}
  .donut-legend{display:flex;gap:1.5rem;flex-wrap:wrap;justify-content:center}
  .legend-item{display:flex;align-items:center;gap:.4rem;font-size:.8rem}
  .legend-dot{width:10px;height:10px;border-radius:50%}
  .type-bars{display:flex;flex-direction:column;gap:.55rem;max-height:320px;overflow-y:auto;padding-right:.5rem}
  .type-bar-row{display:grid;grid-template-columns:170px 1fr 60px;align-items:center;gap:.75rem}
  .type-name{font-size:.78rem;color:#cbd5e1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
  .bar-track{height:14px;background:#0f172a;border-radius:99px;overflow:hidden}
  .bar-inner{height:100%;background:linear-gradient(90deg,#2563eb,#38bdf8);border-radius:99px;transition:width .4s}
  .bar-rate{font-size:.75rem;color:#94a3b8;text-align:right}
  .table-wrap{background:#1e293b;border:1px solid #334155;border-radius:12px;overflow:hidden;margin-bottom:2rem}
  .table-wrap h2{padding:1rem 1.5rem;border-bottom:1px solid #334155;margin:0}
  table{width:100%;border-collapse:collapse}
  thead tr{background:#0f172a}
  th{padding:.6rem 1rem;text-align:left;font-size:.75rem;color:#64748b;text-transform:uppercase;letter-spacing:.05em;white-space:nowrap}
  td{padding:.55rem 1rem;border-bottom:1px solid #1e293b;vertical-align:top}
  .row-even td{background:#1e293b} .row-odd td{background:#172033}
  tr:last-child td{border-bottom:none}
  .idx-cell{color:#475569;font-size:.75rem;text-align:center;width:42px}
  .suite-cell{color:#94a3b8;font-size:.78rem;max-width:280px}
  .badge{display:inline-block;padding:.2rem .6rem;border-radius:6px;font-size:.72rem;font-weight:700;letter-spacing:.04em}
  .pass-badge{background:#14532d;color:#4ade80}
  .fail-badge{background:#450a0a;color:#f87171}
  .type-chip{background:#1e3a5f;color:#38bdf8;padding:.15rem .5rem;border-radius:5px;font-size:.72rem}
  .pass-text{color:#22c55e;font-weight:600} .fail-text{color:#ef4444;font-weight:600}
  .bar-wrap{display:inline-block;width:80px;height:8px;background:#0f172a;border-radius:99px;vertical-align:middle;margin-left:.4rem;overflow:hidden}
  .bar-fill{height:100%;background:#22c55e;border-radius:99px}
  .err-summary{cursor:pointer;color:#f87171;font-size:.78rem;margin-bottom:.25rem}
  .err-pre{background:#0f172a;border:1px solid #334155;border-radius:6px;padding:.75rem;font-size:.72rem;color:#fca5a5;white-space:pre-wrap;word-break:break-all;max-height:200px;overflow-y:auto}
  .footer{text-align:center;padding:2rem;color:#475569;font-size:.8rem;border-top:1px solid #1e293b;margin-top:1rem}
</style>
</head>
<body>

<div class="header">
  <h1>📱 ShadeScanAI – Appium Mobile E2E Report</h1>
  <p class="meta">Generated: ${genTime} &nbsp;|&nbsp; Runner: Appium + UIAutomator2 (Android API 29)</p>
</div>

<div class="container">

  <div class="cards">
    <div class="card"><div class="card-label">Total Tests</div><div class="card-value blue">${total}</div></div>
    <div class="card"><div class="card-label">Passed</div><div class="card-value green">${passed}</div></div>
    <div class="card"><div class="card-label">Failed</div><div class="card-value red">${failed}</div></div>
    <div class="card"><div class="card-label">Pass Rate</div><div class="card-value ${parseFloat(passRate) >= 90 ? 'green' : parseFloat(passRate) >= 70 ? 'amber' : 'red'}">${passRate}%</div></div>
    <div class="card"><div class="card-label">Total Duration</div><div class="card-value amber">${totalMs} ms</div></div>
    <div class="card"><div class="card-label">Categories</div><div class="card-value blue">${Object.keys(byCategory).length}</div></div>
  </div>

  <div class="charts-row">
    <div class="chart-box">
      <h2>Pass / Fail</h2>
      <div class="donut-wrap">
        ${donut}
        <div class="donut-legend">
          <span class="legend-item"><span class="legend-dot" style="background:#22c55e"></span>Passed (${passed})</span>
          <span class="legend-item"><span class="legend-dot" style="background:#ef4444"></span>Failed (${failed})</span>
        </div>
      </div>
    </div>
    <div class="chart-box">
      <h2>Pass Rate by Category</h2>
      <div class="type-bars">${categoryChart}</div>
    </div>
  </div>

  <div class="table-wrap">
    <h2>Mobile Categories Summary</h2>
    <table>
      <thead><tr>
        <th>Category</th><th>Total</th><th>Passed</th><th>Failed</th><th>Pass Rate</th><th>Duration</th>
      </tr></thead>
      <tbody>${typeSummaryRows}</tbody>
    </table>
  </div>

  <div class="table-wrap">
    <h2>All Test Results (${total})</h2>
    <table>
      <thead><tr>
        <th>#</th><th>Category</th><th>Test Case Name</th><th>Type</th><th>Status</th><th>Duration</th><th>Error Details</th>
      </tr></thead>
      <tbody>${testRows}</tbody>
    </table>
  </div>

</div>

<div class="footer">
  ShadeScanAI Appium Suite &nbsp;•&nbsp; ${total} assertions across 11 categories &nbsp;•&nbsp; ${genTime}
</div>

</body>
</html>`;

  const outPath = path.join(outDir, 'execution-report.html');
  fs.writeFileSync(outPath, html, 'utf8');
  console.log(`📄 HTML Execution Report written to: ${outPath}`);
  return outPath;
}

function buildDonut(passed, failed) {
  const total = passed + failed;
  if (total === 0) return '<svg width="160" height="160"></svg>';
  const r = 60, cx = 80, cy = 80, stroke = 22;
  const circ = 2 * Math.PI * r;
  const passArc = (passed / total) * circ;
  const failArc = (failed / total) * circ;
  return `<svg width="160" height="160" viewBox="0 0 160 160">
    <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="#0f172a" stroke-width="${stroke}"/>
    <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="#22c55e" stroke-width="${stroke}"
      stroke-dasharray="${passArc} ${circ}" stroke-dashoffset="${circ * 0.25}" stroke-linecap="round" transform="rotate(-90 ${cx} ${cy})"/>
    ${failed > 0 ? `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="#ef4444" stroke-width="${stroke}"
      stroke-dasharray="${failArc} ${circ}" stroke-dashoffset="${circ * 0.25 - passArc}" stroke-linecap="round" transform="rotate(-90 ${cx} ${cy})"/>` : ''}
    <text x="${cx}" y="${cy}" text-anchor="middle" dominant-baseline="middle" fill="#f1f5f9" font-size="22" font-weight="700">${((passed/total)*100).toFixed(0)}%</text>
    <text x="${cx}" y="${cy + 18}" text-anchor="middle" fill="#64748b" font-size="10">pass rate</text>
  </svg>`;
}

function buildCategoryChart(byCategory) {
  return Object.entries(byCategory)
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([cat, s]) => {
      const rate = s.total > 0 ? ((s.pass / s.total) * 100).toFixed(1) : '0.0';
      return `<div class="type-bar-row">
        <div class="type-name" title="${escHtml(cat)}">${escHtml(cat)}</div>
        <div class="bar-track"><div class="bar-inner" style="width:${rate}%"></div></div>
        <div class="bar-rate">${rate}%</div>
      </div>`;
    }).join('\n');
}

function escHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

module.exports = { generateHtmlReport };
