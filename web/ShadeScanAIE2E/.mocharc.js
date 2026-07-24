'use strict';
/**
 * Mocha configuration for ShadeScanAI E2E suite
 */
module.exports = {
  spec:        'tests/**/*.test.js',
  timeout:     30000,
  slow:        5000,
  reporter:    'spec',
  require:     ['utils/excelReporter.js'],
  exit:        true,
  parallel:    false,
};
