/**
 * Purpose: Playwright configuration. Starts the Spring Boot backend (gradle bootRun, seeds
 * 12,000 instruments + 15,000 trades) and the Vite dev server, then runs the e2e tests.
 * Writes an HTML report, JUnit XML and JSON results (plus screenshots/videos) that the CI
 * workflow uploads as 1-day artifacts.
 */
import { defineConfig, devices } from '@playwright/test';

// Use GRADLE_CMD=./gradlew if you generated a Gradle wrapper.
const gradle = process.env.GRADLE_CMD ?? 'gradle';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['json', { outputFile: 'test-results/results.json' }],
  ],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    screenshot: 'on',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: gradle + ' :backend:bootRun',
      cwd: '..',
      url: 'http://127.0.0.1:8080/api/instruments?range=%5B0%2C0%5D',
      timeout: 300_000,
      reuseExistingServer: !process.env.CI,
    },
    {
      command: 'npm run dev',
      url: 'http://127.0.0.1:5173',
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
});
