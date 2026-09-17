import { test, expect } from '@playwright/test';

test.describe('P14-F: Keyboard Navigation & Focus Management', () => {
  test('Skip-to-content link functionality', async ({ page }) => {
    await page.goto('http://localhost:3000/');
    await page.waitForLoadState('networkidle');

    // First Tab should focus the skip-to-content link
    await page.keyboard.press('Tab');
    const skipLink = page.locator('.skip-link');
    await expect(skipLink).toBeFocused();
    await expect(skipLink).toBeVisible();

    // Activating skip link with Enter moves focus to #main-content
    await page.keyboard.press('Enter');
    const mainContent = page.locator('#main-content');
    await expect(mainContent).toBeFocused();
  });

  test('Primary Header keyboard navigation and view switching', async ({ page }) => {
    await page.goto('http://localhost:3000/');
    await page.waitForLoadState('networkidle');

    // Tab through skip-link to brand logo
    await page.keyboard.press('Tab'); // skip link
    await page.keyboard.press('Tab'); // brand button

    const brandBtn = page.locator('button[aria-label="PVK Cinemas Home"]');
    await expect(brandBtn).toBeFocused();

    // Tab to search bar
    await page.keyboard.press('Tab');
    await expect(page.locator('input[type="search"]')).toBeFocused();

    // Tab to city selector
    await page.keyboard.press('Tab');
    await expect(page.locator('select[aria-label="Select City"]')).toBeFocused();

    // Tab to Movies nav link in header
    await page.keyboard.press('Tab');
    const moviesNav = page.locator('header button:has-text("Movies")');
    await expect(moviesNav).toBeFocused();

    // Activate with Enter
    await page.keyboard.press('Enter');
    await expect(page.locator('[data-testid="movie-listing-view"]')).toBeVisible();

    // Tab to Theatres nav link in header
    await page.keyboard.press('Tab');
    const theatresNav = page.locator('header button:has-text("Theatres")');
    await expect(theatresNav).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.locator('[data-testid="city-theatres-view"]')).toBeVisible();
  });

  test('Authentication form keyboard focus and validation alert announcement', async ({ page }) => {
    await page.goto('http://localhost:3000/');
    await page.waitForLoadState('networkidle');

    // Navigate to login
    await page.evaluate(() => {
      (window as any).__pvk_navigate('login');
    });
    await expect(page.locator('[data-testid="auth-view"]')).toBeVisible();

    // Focus email input and fill invalid credentials to trigger submission error
    const emailInput = page.locator('#login-email');
    await emailInput.focus();
    await page.keyboard.type('invalid_user@example.test');

    // Tab to password
    await page.keyboard.press('Tab');
    const passwordInput = page.locator('#login-password');
    await expect(passwordInput).toBeFocused();
    await page.keyboard.type('WrongPassword!');

    // Tab to submit button
    await page.keyboard.press('Tab');
    const submitBtn = page.locator('button[type="submit"]:has-text("Sign In")');
    await expect(submitBtn).toBeFocused();

    // Submit form with keyboard Enter
    await page.keyboard.press('Enter');

    // Alert banner should appear with role="alert"
    const errorAlert = page.locator('div[role="alert"]');
    await expect(errorAlert).toBeVisible({ timeout: 10_000 });
  });

  test('UI-03 Movie Details and UI-08 Seat Availability keyboard traversal', async ({ page }) => {
    await page.goto('http://localhost:3000/');
    await page.waitForLoadState('networkidle');

    // Wait for movie cards to appear on home page
    const movieCards = page.locator('[data-testid^="movie-card-"]');
    await expect(movieCards.first()).toBeVisible({ timeout: 10_000 });

    // Navigate to Movie Details by clicking the first movie card
    await movieCards.first().click();
    await page.waitForLoadState('networkidle');

    // Confirm UI-03 Movie Details view
    await expect(page.locator('[data-testid="movie-details-view"]')).toBeVisible();

    // Confirm UI-03 structural hierarchy (Movie -> Theatre -> Screen -> Showtimes)
    await expect(page.locator('[data-testid="theatre-grouped-showtimes-section"]')).toBeVisible();
    await expect(page.locator('text=Morning')).toHaveCount(0);
    await expect(page.locator('text=Afternoon')).toHaveCount(0);
    await expect(page.locator('text=Evening')).toHaveCount(0);

    // Verify showtime pills have accessible names and can be activated with keyboard
    const showtimePills = page.locator('[data-testid^="showtime-pill-"]');
    const pillCount = await showtimePills.count();

    if (pillCount > 0) {
      const firstPill = showtimePills.first();
      const accessibleName = await firstPill.getAttribute('aria-label');
      expect(accessibleName).toBeTruthy();
      expect(accessibleName).toMatch(/Show at .* in .*/);

      // Focus the pill and press Enter to navigate to UI-08 Seat Availability
      await firstPill.focus();
      await expect(firstPill).toBeFocused();
      await page.keyboard.press('Enter');
      await page.waitForLoadState('networkidle');

      // Verify UI-08 Seat Availability
      await expect(page.locator('[data-testid="seat-availability-view"]')).toBeVisible();

      // Mandatory boundary banner
      await expect(page.getByText(/Seat availability preview only/i)).toBeVisible();
      await expect(page.getByText(/Online ticket booking is not supported/i)).toBeVisible();

      // Check that seats are non-interactive presentation images with accessible labels
      const firstSeat = page.locator('[data-testid^="seat-node-"]').first();
      await expect(firstSeat).toBeVisible();
      await expect(firstSeat).toHaveAttribute('role', 'img');
      const label = await firstSeat.getAttribute('aria-label');
      expect(label).toMatch(/Seat [A-Z]-[0-9]+, (AVAILABLE|BOOKED|BLOCKED)/);

      // Verify absence of booking/checkout/payment buttons
      await expect(page.locator('button:has-text("Book Now")')).toHaveCount(0);
      await expect(page.locator('button:has-text("Reserve")')).toHaveCount(0);
      await expect(page.locator('button:has-text("Checkout")')).toHaveCount(0);
    }
  });
});
