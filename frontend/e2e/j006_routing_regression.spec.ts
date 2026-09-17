import { test, expect } from './fixtures/browser_fixtures';

/**
 * PVK Cinemas — J006: URL / Hash Routing & Navigation E2E Verification
 * Verifies lightweight hash routing (IMP-015 / Section 8):
 * - URL synchronizes with navigation state
 * - Browser Back / Forward buttons work properly
 * - Refreshing a valid route retains the active view
 * - Deep links load directly
 * - Invalid routes fall back gracefully to Home without crashing
 */

test.describe('J006: URL Hash Routing & Navigation', () => {

  test('ROUTE-01: Home -> Movie Details -> Back returns to Home', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Verify initial URL is either / or /#/home
    expect(page.url()).toMatch(/\/(#\/home)?$/);

    // Click first movie card
    const movieCards = page.locator('[data-testid^="movie-card-"]');
    await expect(movieCards.first()).toBeVisible({ timeout: 10_000 });
    const movieTitleText = (await movieCards.first().locator('h4').textContent()) || '';
    await movieCards.first().click();
    await page.waitForLoadState('networkidle');

    // Verify hash changed to #/movie/<id>
    expect(page.url()).toContain('#/movie/');
    await expect(page.locator('h1')).toContainText(movieTitleText);

    // Browser Back button
    await page.goBack();
    await page.waitForLoadState('networkidle');

    // Verify URL returned to home and home movie cards are visible
    expect(page.url()).toMatch(/\/(#\/home)?$/);
    await expect(movieCards.first()).toBeVisible();
  });

  test('ROUTE-02: Search -> Movie Details -> Back returns to Search', async ({ page }) => {
    // Navigate directly to Search via hash
    await page.goto('/#/search');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toContain('#/search');
    const searchView = page.locator('[data-testid="search-results-view"]');
    await expect(searchView).toBeVisible();

    const searchInput = page.getByRole('searchbox', { name: 'Search query input' });
    await expect(searchInput).toBeVisible();

    // Search for Inception
    await searchInput.fill('Inception');
    await page.keyboard.press('Enter');
    await page.waitForTimeout(1000);

    // If search cards appear, click the first one
    const searchCards = page.locator('[data-testid^="search-card-"]');
    if (await searchCards.first().isVisible()) {
      await searchCards.first().click();
      await page.waitForLoadState('networkidle');

      // Verify route is movie details
      expect(page.url()).toContain('#/movie/');

      // Browser Back
      await page.goBack();
      await page.waitForLoadState('networkidle');

      // Verify returned to search
      expect(page.url()).toContain('#/search');
      await expect(searchView).toBeVisible();
    }
  });

  test('ROUTE-03: Refresh on valid route retains route', async ({ page }) => {
    await page.goto('/#/search');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toContain('#/search');
    const searchView = page.locator('[data-testid="search-results-view"]');
    await expect(searchView).toBeVisible();

    // Reload page
    await page.reload();
    await page.waitForLoadState('networkidle');

    // URL should still be #/search and Search view should still be rendered
    expect(page.url()).toContain('#/search');
    await expect(searchView).toBeVisible();
  });

  test('ROUTE-04: Browser Forward navigates forward correctly', async ({ page }) => {
    await page.goto('/#/home');
    await page.waitForLoadState('networkidle');

    const movieCards = page.locator('[data-testid^="movie-card-"]');
    await expect(movieCards.first()).toBeVisible({ timeout: 10_000 });
    await movieCards.first().click();
    await page.waitForLoadState('networkidle');

    expect(page.url()).toContain('#/movie/');
    const movieUrl = page.url();

    // Back to home
    await page.goBack();
    await page.waitForLoadState('networkidle');
    expect(page.url()).toMatch(/\/(#\/home)?$/);

    // Forward to movie
    await page.goForward();
    await page.waitForLoadState('networkidle');
    expect(page.url()).toBe(movieUrl);
    await expect(page.locator('h1')).toBeVisible();
  });

  test('ROUTE-05: Invalid route safely falls back to Home', async ({ page }) => {
    // Navigate to an invalid/garbage route
    await page.goto('/#/invalid-xyz-route-9999');
    await page.waitForLoadState('networkidle');

    // Should not crash and should render Home view elements
    await expect(page.locator('header')).toBeVisible();
    const movieCards = page.locator('[data-testid^="movie-card-"]');
    await expect(movieCards.first()).toBeVisible({ timeout: 10_000 });
  });

});
