import { test, expect } from './fixtures/browser_fixtures';

/**
 * PVK Cinemas — J002: Customer Search & AI Discovery E2E Journey
 * Authoritative Basis: Test Strategy §17, SRS FR-090–FR-098, Architecture §11, §12.
 *
 * Flow: [Header / UI-01 Search Bar] -> [UI-04 Search Results] -> [Movie Details]
 * Architecture Verified:
 * React Frontend -> Spring Boot /api/v1/search -> FastAPI Search Service (Port 8001) -> MySQL
 */

test.describe('J002: Customer Search & AI Discovery', () => {

  test('J002-01: Global search query proxies to AI search service and renders ranked candidates', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') consoleErrors.push(msg.text());
    });

    // 1. Open Homepage
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // 2. Submit query via Header global search bar
    const searchInput = page.locator('header input[type="search"]');
    await expect(searchInput).toBeVisible();
    await searchInput.fill('Inception');
    await searchInput.press('Enter');
    await page.waitForLoadState('networkidle');

    // 3. Verify transition to Search Results View (UI-04)
    const searchView = page.locator('[data-testid="search-results-view"]');
    await expect(searchView).toBeVisible();

    // 4. Verify search results header and ranked candidates rendered
    await expect(page.getByText(/Found .* matching candidates? for "Inception"/i)).toBeVisible({ timeout: 10_000 });

    const searchCards = page.locator('[data-testid^="search-card-"]');
    await expect(searchCards.first()).toBeVisible({ timeout: 10_000 });
    const count = await searchCards.count();
    expect(count).toBeGreaterThan(0);

    // Verify entity details and rank badge on first candidate
    const firstCard = searchCards.first();
    await expect(firstCard.locator('.badge-crimson, .badge-gold')).toBeVisible();
    await expect(firstCard.getByText(/Rank #/i)).toBeVisible();

    // 5. Click first candidate card -> navigates to Movie Details (UI-03)
    await firstCard.click();
    await page.waitForLoadState('networkidle');

    // Verify arrived at Movie Details
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Synopsis' })).toBeVisible();

    // 6. Return to Search View via Header search bar
    await searchInput.fill('Multiplex');
    await searchInput.press('Enter');
    await page.waitForLoadState('networkidle');

    await expect(searchView).toBeVisible();
    await expect(page.getByText(/Found .* matching candidates? for "Multiplex"/i)).toBeVisible({ timeout: 10_000 });

    // 7. Test in-page search form input
    const searchForm = page.locator('[data-testid="search-results-view"] form');
    const inPageInput = searchForm.locator('input[type="search"]');
    await inPageInput.fill('Action');
    await searchForm.locator('button[type="submit"]').click();
    await page.waitForLoadState('networkidle');
    await expect(page.getByText(/Found .* matching candidates? for "Action"/i)).toBeVisible({ timeout: 10_000 });

    // Verify zero critical console errors
    const criticalErrors = consoleErrors.filter((e) => !e.includes('favicon'));
    expect(criticalErrors).toHaveLength(0);
  });

});
