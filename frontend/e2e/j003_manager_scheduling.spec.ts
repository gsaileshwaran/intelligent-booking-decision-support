import { test, expect } from './fixtures/browser_fixtures';

/**
 * PVK Cinemas — J003: Theatre Manager Operational Scheduling E2E Journey
 * Authoritative Basis: Test Strategy §15, SRS FR-024, FR-070–FR-075, BR-001–BR-006.
 *
 * Flow: [UI-11 Manager Dashboard] -> [UI-14 Show Scheduling] -> [Schedule Show] -> [BR-004 Overlap Conflict] -> [403 Scope Isolation]
 * Invariants:
 * 1. Manager A can only manage assigned multiplex (Theatre 1).
 * 2. BR-004 prevents overlapping scheduled shows on the same screen.
 * 3. Cross-theatre access returns 403 Forbidden.
 */

test.describe('J003: Theatre Manager Operational Scheduling', () => {

  test('J003-01: Manager A schedules show, verifies BR-004 overlap conflict, and cross-theatre 403 denial', async ({ authenticatedPage, page }) => {
    test.setTimeout(60_000);

    page.on('console', msg => console.log(`BROWSER [${msg.type()}]:`, msg.text()));
    page.on('pageerror', err => console.log(`BROWSER ERROR:`, err.message));
    page.on('requestfailed', req => console.log(`REQUEST FAILED:`, req.url(), req.failure()));
    page.on('response', res => {
      if (res.status() >= 400) console.log(`HTTP ${res.status()}: ${res.url()}`);
    });

    // 1. Authenticate as Manager A (Assigned strictly to Theatre 1)
    await authenticatedPage('manager_a');

    // 2. Navigate to Manager Dashboard via Header Manager button
    const managerNavButton = page.getByRole('button', { name: 'Manager' }).first();
    await expect(managerNavButton).toBeVisible();
    await managerNavButton.click();
    await page.waitForLoadState('networkidle');

    // 3. Verify Manager Dashboard (UI-11)
    const managerDashboard = page.locator('[data-testid="manager-dashboard-view"]');
    await expect(managerDashboard).toBeVisible();
    await expect(page.getByText(/Theatre Operations Portal/i)).toBeVisible();

    // 4. Navigate to Show Scheduler (UI-14)
    const scheduleShowButton = page.locator('button:has-text("Schedule Show")').first();
    await expect(scheduleShowButton).toBeVisible();
    await scheduleShowButton.click();
    await page.waitForLoadState('networkidle');

    const showManagementView = page.locator('[data-testid="show-management-view"]');
    await expect(showManagementView).toBeVisible();
    await expect(page.locator('h1')).toContainText('Show Scheduler');

    // 5. Click "Schedule New Show" to open the scheduling modal
    const scheduleNewButton = page.locator('button:has-text("Schedule New Show")');
    await expect(scheduleNewButton).toBeVisible();
    await scheduleNewButton.click();

    // Verify modal opened
    await expect(page.locator('h3:has-text("Schedule New Screening")')).toBeVisible();

    // 6. Select movie, language, screen, and times
    const movieSelect = page.locator('#show-movie');
    await expect(movieSelect).toBeVisible();
    await movieSelect.selectOption({ index: 1 });

    // Wait for language options to populate and select
    const langSelect = page.locator('#show-lang');
    await expect(langSelect.locator('option').nth(1)).toBeAttached({ timeout: 5000 });
    await langSelect.selectOption({ index: 1 });

    // Select screen
    const screenSelect = page.locator('#show-screen');
    await expect(screenSelect.locator('option').nth(1)).toBeAttached({ timeout: 5000 });
    await screenSelect.selectOption({ index: 1 });

    // Set unique non-overlapping future times using dynamic offset to avoid collisions across repeated test runs
    const futureDate = new Date();
    const dayOffset = 100 + (Math.floor(Date.now() / 1000) % 500);
    futureDate.setDate(futureDate.getDate() + dayOffset);
    futureDate.setHours(14, 0, 0, 0);
    const startString = futureDate.toISOString().slice(0, 16);

    futureDate.setHours(16, 30, 0, 0);
    const endString = futureDate.toISOString().slice(0, 16);

    await page.locator('#show-start').fill(startString);
    await page.locator('#show-end').fill(endString);

    // Submit show creation
    const submitBtn = page.locator('button[type="submit"]:has-text("Confirm Schedule")');
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();
    await page.waitForLoadState('networkidle');

    // Verify success toast
    await expect(page.getByText(/Show scheduled successfully/i)).toBeVisible({ timeout: 10_000 });

    // 7. NEGATIVE TEST 1: BR-004 Conflict Overlap Pre-Validation & Rejection
    // Re-open scheduling modal
    await scheduleNewButton.click();
    await expect(page.locator('h3:has-text("Schedule New Screening")')).toBeVisible();

    // Select the exact same movie, screen, and overlapping time range
    await movieSelect.selectOption({ index: 1 });
    await screenSelect.selectOption({ index: 1 });
    await page.locator('#show-start').fill(startString);
    await page.locator('#show-end').fill(endString);

    // Verify pre-validation banner warns about scheduling conflict (BR-004)
    const conflictWarning = page.locator('.banner-warning');
    await expect(conflictWarning).toBeVisible();
    await expect(conflictWarning).toContainText(/Scheduling Conflict Detected/i);
    await expect(conflictWarning).toContainText(/BR-004/i);

    // Verify submit button is disabled in UI due to BR-004 conflict
    await expect(submitBtn).toBeDisabled();

    // Close modal via Cancel button
    const cancelBtn = page.locator('button:has-text("Cancel")');
    await cancelBtn.click();

    // 8. NEGATIVE TEST 2: Horizontal Theatre Scope Isolation (Manager A blocked from Theatre 2)
    const token = await page.evaluate(() => localStorage.getItem('pvk_token'));
    expect(token).toBeTruthy();

    const crossTheatreResp = await page.request.get('http://localhost:8080/api/v1/manager/theatres/2/screens', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    // Manager A must be rejected with 403 Forbidden for Theatre 2
    expect(crossTheatreResp.status()).toBe(403);
  });

});
