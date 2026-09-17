import { chromium } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const ARTIFACT_DIR = 'C:/Users/admin/.gemini/antigravity-ide/brain/0adefcec-8890-43d6-ba30-069575b5fdc7/scratch/screenshots';
fs.mkdirSync(ARTIFACT_DIR, { recursive: true });

async function runTest() {
  console.log('Launching local Google Chrome...');
  const browser = await chromium.launch({
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    headless: true
  });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  page.on('console', msg => console.log(`[Browser Console ${msg.type()}]:`, msg.text()));
  page.on('pageerror', err => console.error('[Browser Error]:', err.message));

  console.log('Navigating directly to Interstellar Movie Details: http://localhost:3000/#/movie/680...');
  await page.goto('http://localhost:3000/#/movie/680');
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1500);

  // If city selector modal is visible, click Chennai
  const chennaiBtn = page.locator('text=Chennai').first();
  if (await chennaiBtn.isVisible().catch(() => false)) {
    console.log('Selecting Chennai from city selector modal...');
    await chennaiBtn.click();
    await page.waitForTimeout(1000);
  }

  // Ensure city is set in localStorage if needed
  await page.evaluate(() => {
    localStorage.setItem('pvk_city_id', '1');
    localStorage.setItem('pvk_city_name', 'Chennai');
  });

  // Wait for Movie Title to load
  await page.waitForSelector('text=Interstellar', { timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(1500);

  console.log('Capturing Movie Details with Decision Support Panel...');
  await page.screenshot({ path: path.join(ARTIFACT_DIR, '01_decision_panel_interstellar.png'), fullPage: false });

  // Click 'Best Seats' priority button
  const bestSeatsBtn = page.locator('#priority-best_seats');
  if (await bestSeatsBtn.isVisible().catch(() => false)) {
    console.log('Clicking Best Seats priority button...');
    await bestSeatsBtn.click();
    await page.waitForTimeout(400);
  }

  // Click 'Find Best Shows'
  const findShowsBtn = page.locator('#btn-find-best-shows');
  if (await findShowsBtn.isVisible().catch(() => false)) {
    console.log('Clicking Find Best Shows button...');
    await findShowsBtn.click();
    await page.waitForTimeout(2500);
  }

  console.log('Capturing Ranked Candidate Options...');
  await page.screenshot({ path: path.join(ARTIFACT_DIR, '02_decision_results_ranked.png'), fullPage: false });

  // Check for candidate cards
  const topPickBadge = page.locator('text=TOP PICK').first();
  const hasTopPick = await topPickBadge.isVisible().catch(() => false);
  console.log('Top Pick badge visible:', hasTopPick);

  const candidateCards = page.locator('[data-testid^="recommendation-candidate-"]');
  const count = await candidateCards.count().catch(() => 0);
  console.log(`Ranked candidate cards visible on screen: ${count}`);

  if (count > 0) {
    const firstCardText = await candidateCards.first().innerText();
    console.log('--- Top Pick Card Content Preview ---');
    console.log(firstCardText.split('\n').slice(0, 8).join(' | '));
  }

  await browser.close();
  console.log('Visual verification completed successfully!');
}

runTest().catch(err => {
  console.error('Visual test failed:', err);
  process.exit(1);
});
