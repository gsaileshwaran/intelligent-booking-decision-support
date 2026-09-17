import { chromium } from '@playwright/test';

async function check() {
  const browser = await chromium.launch({
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    headless: true
  });
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('BROWSER LOG:', msg.text()));

  await page.goto('http://localhost:3000/#/movie/680');
  await page.waitForTimeout(4000);

  const cardsCount = await page.locator('[data-testid^="recommendation-candidate-"]').count();
  console.log('Cards count on initial load:', cardsCount);

  if (cardsCount > 0) {
    const cardText = await page.locator('[data-testid^="recommendation-candidate-"]').first().innerText();
    console.log('=== FIRST CARD CONTENT ===\n' + cardText);
  } else {
    // Check if showRec was received
    const evalResult = await page.evaluate(() => {
      return {
        hasTitle: document.body.innerText.includes('Interstellar'),
        hasFindYourBestShow: document.body.innerText.includes('Find Your Best Show'),
        hasEvaluating: document.body.innerText.includes('Evaluating all shows'),
        hasNoShows: document.body.innerText.includes('No matching shows') || document.body.innerText.includes('No shows'),
      };
    });
    console.log('Page state evaluation:', evalResult);
  }

  await browser.close();
}

check().catch(console.error);
