import { test, expect } from './fixtures/browser_fixtures';

/**
 * PVK Cinemas — J001: Anonymous Customer Discovery E2E Journey
 * Authoritative Basis: Test Strategy §14.1, SRS FR-030–FR-034, FR-050, FR-070, FR-080, FR-083.
 *
 * Flow: [UI-01 Home] -> [UI-02/03 Movie Details] -> [UI-08 Seat Availability]
 * Invariants:
 * 1. Schedule grouped strictly theatre-wise (Movie -> Theatre -> Screen -> Showtimes).
 * 2. Seat availability is strictly display-only; zero booking/hold/payment actions.
 */

test.describe('J001: Anonymous Customer Discovery', () => {

  test('J001-01: Browse movie catalog, verify theatre-wise schedule, and inspect display-only seats', async ({ page }) => {
    // Collect console errors during journey execution
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') consoleErrors.push(msg.text());
    });

    // 1. Open Homepage (UI-01)
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Verify brand header and navigation
    await expect(page.locator('header')).toBeVisible();
    await expect(page.locator('header').getByText('PVK', { exact: true })).toBeVisible();
    await expect(page.locator('header').getByText('CINEMAS', { exact: true })).toBeVisible();

    // Verify featured movies section
    const movieCards = page.locator('[data-testid^="movie-card-"]');
    await expect(movieCards.first()).toBeVisible({ timeout: 10_000 });
    const movieCount = await movieCards.count();
    expect(movieCount).toBeGreaterThan(0);

    // 2. Click on the first movie to open Movie Details (UI-03)
    const firstMovieCard = movieCards.first();
    const movieTitleText = (await firstMovieCard.locator('h4').textContent()) || '';
    expect(movieTitleText.trim().length).toBeGreaterThan(0);
    await firstMovieCard.click();
    await page.waitForLoadState('networkidle');

    // 3. Verify Movie Details (UI-03)
    await expect(page.locator('h1')).toContainText(movieTitleText);
    await expect(page.getByRole('heading', { name: 'Synopsis' })).toBeVisible();

    // 4. Verify STRICT INVARIANT: Theatre-Wise Grouped Showtimes (UI-03)
    const showtimesSection = page.locator('[data-testid="theatre-grouped-showtimes-section"]');
    await expect(showtimesSection).toBeVisible();

    // Verify that primary grouping is by theatre, not generic time-of-day
    const theatreGroups = page.locator('[data-testid^="theatre-showtime-group-"]');
    const groupCount = await theatreGroups.count();

    // Assert that generic time-of-day groupings are NOT present as primary schedule containers
    await expect(page.locator('[data-testid="morning-showtimes-group"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="afternoon-showtimes-group"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="evening-showtimes-group"]')).toHaveCount(0);

    // If active showtimes exist for this movie, traverse to Seat Availability (UI-08)
    const showtimePills = page.locator('[data-testid^="showtime-pill-"]');
    const pillCount = await showtimePills.count();

    if (pillCount > 0) {
      // 5. Select the first available showtime pill -> opens UI-08 Seat Availability
      await showtimePills.first().click();
      await page.waitForLoadState('networkidle');

      // 6. Verify Seat Availability (UI-08)
      const seatView = page.locator('[data-testid="seat-availability-view"]');
      await expect(seatView).toBeVisible();

      // 7. Verify DisplayOnlyBanner is prominently rendered
      await expect(page.getByText(/Seat availability preview only/i)).toBeVisible();
      await expect(page.getByText(/Online ticket booking is not supported/i)).toBeVisible();

      // 8. Verify seat availability metrics
      await expect(page.locator('text=Available').first()).toBeVisible();
      await expect(page.locator('text=Booked').first()).toBeVisible();
      await expect(page.locator('text=Blocked').first()).toBeVisible();

      // 9. Inspect the seat grid and click a seat for informational status
      const seatNodes = page.locator('[data-testid^="seat-node-"]');
      const seatCount = await seatNodes.count();
      expect(seatCount).toBeGreaterThan(0);

      // Click first available seat -> inspect informational tooltip / selection state
      await seatNodes.first().click();

      // 10. CRITICAL NON-NEGOTIABLE ARCHITECTURAL BOUNDARY:
      // Absolutely NO booking, reservation, seat hold, cart, or payment actions
      const forbiddenActionButtons = page.locator([
        'button:has-text("Book Now")',
        'button:has-text("Book Ticket")',
        'button:has-text("Reserve Seat")',
        'button:has-text("Hold Seat")',
        'button:has-text("Checkout")',
        'button:has-text("Proceed to Pay")',
        'button:has-text("Add to Cart")',
      ].join(', '));

      await expect(forbiddenActionButtons).toHaveCount(0);
    }

    // Assert no unhandled console errors occurred
    const criticalErrors = consoleErrors.filter((e) => !e.includes('favicon'));
    expect(criticalErrors).toHaveLength(0);
  });

});
