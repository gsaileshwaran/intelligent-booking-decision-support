import { defineConfig, devices } from '@playwright/test';

/**
 * PVK Cinemas — Authoritative P14 Playwright E2E Configuration
 * Configures the test runner for the React 19 SPA frontend (port 3000).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: {
    timeout: 5_000,
  },
  fullyParallel: false, // Maintain deterministic order during journey execution
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1, // Single worker for transactional safety across journeys
  reporter: [
    ['list'],
    ['json', { outputFile: '../tests/evidence/e2e/results.json' }],
  ],
  use: {
    baseURL: process.env.PVK_FRONTEND_URL || 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
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
  outputDir: '../tests/evidence/e2e/test-artifacts',
});
