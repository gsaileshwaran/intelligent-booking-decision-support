import { chromium } from '@playwright/test';
import path from 'path';

const ARTIFACT_DIR = 'C:/Users/admin/.gemini/antigravity-ide/brain/0adefcec-8890-43d6-ba30-069575b5fdc7';

async function capture() {
  const browser = await chromium.launch({
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    headless: true
  });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1100 } });
  const page = await context.newPage();

  console.log('Opening Interstellar Movie Details...');
  await page.goto('http://localhost:3000/#/movie/680');
  await page.waitForTimeout(4000);

  // Take screenshot of decision panel + top candidate cards
  await page.screenshot({ path: path.join(ARTIFACT_DIR, 'decision_support_panel_live.png'), fullPage: false });
  console.log('Captured decision_support_panel_live.png');

  // Change to 4 seats, Best Seats priority
  const seatsInput = page.locator('input[type="number"]').first();
  await seatsInput.fill('4');
  await page.waitForTimeout(300);

  const bestSeatsBtn = page.locator('#priority-best_seats');
  await bestSeatsBtn.click();
  await page.waitForTimeout(300);

  const findBtn = page.locator('#btn-find-best-shows');
  await findBtn.click();
  await page.waitForTimeout(3000);

  await page.screenshot({ path: path.join(ARTIFACT_DIR, 'best_seats_4party_ranked.png'), fullPage: false });
  console.log('Captured best_seats_4party_ranked.png');

  await browser.close();
}

capture().catch(console.error);
