import { test, expect } from '@playwright/test';

const VIEWPORTS = [
  { name: 'Desktop (1440px)', width: 1440, height: 900 },
  { name: 'Small Desktop (1024px)', width: 1024, height: 768 },
  { name: 'Tablet (768px)', width: 768, height: 1024 },
  { name: 'Mobile (390px)', width: 390, height: 844 },
];

const SCREENS = [
  { id: 'UI-01', name: 'Home', view: 'home' },
  { id: 'UI-02', name: 'Movie Listing', view: 'movies' },
  { id: 'UI-03', name: 'Movie Details', view: 'movie-details', param: 1 },
  { id: 'UI-06', name: 'Theatre Details', view: 'theatre-details', param: 1 },
  { id: 'UI-08', name: 'Seat Availability', view: 'show-seats', param: 1 },
  { id: 'UI-09', name: 'Authentication', view: 'login' },
];

test.describe('P14-F: Responsive Usability & Reflow Verification', () => {
  for (const vp of VIEWPORTS) {
    test(`Verify layout integrity and zero horizontal overflow at ${vp.name}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto('http://localhost:3000/');
      await page.waitForLoadState('networkidle');

      for (const screen of SCREENS) {
        await page.evaluate(({ view, param }) => {
          (window as any).__pvk_navigate(view, param);
        }, { view: screen.view, param: screen.param });

        await page.waitForTimeout(300);

        // Check horizontal overflow: document scrollWidth should not exceed viewport width
        const overflow = await page.evaluate(() => {
          return {
            scrollWidth: document.documentElement.scrollWidth,
            clientWidth: document.documentElement.clientWidth,
            innerWidth: window.innerWidth,
          };
        });

        expect(
          overflow.scrollWidth,
          `Horizontal overflow detected on ${screen.id} (${screen.name}) at ${vp.name}: scrollWidth=${overflow.scrollWidth}, clientWidth=${overflow.clientWidth}`
        ).toBeLessThanOrEqual(overflow.innerWidth + 2);

        // Verify primary header remains visible
        const header = page.locator('header');
        await expect(header).toBeVisible();
      }
    });
  }
});
