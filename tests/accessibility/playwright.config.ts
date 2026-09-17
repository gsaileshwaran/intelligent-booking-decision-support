import { defineConfig, devices } from '@playwright/test';

/**
 * PVK Cinemas — P14-F Accessibility & Usability Test Runner Configuration
 */
export default defineConfig({
  testDir: './',
  timeout: 45_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [
    ['list'],
    ['json', { outputFile: '../evidence/accessibility_results.json' }],
  ],
  use: {
    baseURL: process.env.PVK_FRONTEND_URL || 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    headless: true,
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1280, height: 720 },
      },
    },
  ],
  outputDir: '../evidence/test-artifacts-a11y',
});
