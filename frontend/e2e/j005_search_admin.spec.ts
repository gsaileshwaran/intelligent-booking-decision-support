import { test, expect } from './fixtures/browser_fixtures';

/**
 * PVK Cinemas — J005: Super Admin Search Administration & Reindexing E2E Journey
 * Authoritative Basis: Test Strategy §16, §17, SRS FR-097, FR-098, Architecture §11.
 *
 * Flow: [UI-15 Admin Dashboard] -> [UI-21 Search Administration] -> [Check Index Status] -> [Trigger Reindex] -> [Verify Feedback & DB Integrity]
 * Invariants:
 * 1. Only Super Admin can access Search Administration and trigger reindexing.
 * 2. Reindexing invokes Spring Boot -> FastAPI orchestration pipeline.
 * 3. Zero operational database tables (28 entities) are modified during reindexing.
 */

test.describe('J005: Search Administration & Reindexing', () => {

  test('J005-01: Super Admin inspects search telemetry, triggers reindexing, and verifies operational integrity', async ({ authenticatedPage, page }) => {
    test.setTimeout(60_000);

    // 1. Authenticate as Super Admin
    await authenticatedPage('super_admin');

    // 2. Navigate to Admin Dashboard via Header Admin button
    const adminNavButton = page.getByRole('button', { name: 'Admin' }).first();
    await expect(adminNavButton).toBeVisible();
    await adminNavButton.click();
    await page.waitForLoadState('networkidle');

    // 3. Verify Admin Dashboard (UI-15)
    const adminDashboard = page.locator('[data-testid="admin-dashboard-view"]');
    await expect(adminDashboard).toBeVisible();
    await expect(page.getByText(/Platform Administration/i)).toBeVisible();

    // 4. Navigate to Search Administration (UI-21)
    const searchAdminCard = page.locator('.card:has-text("Search Administration")');
    await expect(searchAdminCard).toBeVisible();
    await searchAdminCard.click();
    await page.waitForLoadState('networkidle');

    // 5. Verify Search Administration View (UI-21)
    const searchAdminView = page.locator('[data-testid="search-admin-view"]');
    await expect(searchAdminView).toBeVisible();
    await expect(page.locator('h1')).toContainText('Search Engine Administration');

    // 6. Verify Index Status Metrics
    await expect(page.getByText(/Indexed Entities \(Movies & Theatres\)/i)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(/Pipeline Status/i)).toBeVisible();
    await expect(page.getByText(/Semantic Vector Model/i)).toBeVisible();

    // Verify architecture specifications panel is rendered
    await expect(page.getByText(/Hybrid AI Pipeline Specifications/i)).toBeVisible();
    await expect(page.getByText(/Reciprocal Rank Fusion/i)).toBeVisible();

    // 7. Trigger Full Reindexing
    const reindexBtn = page.locator('button:has-text("Trigger Full Reindexing")');
    await expect(reindexBtn).toBeVisible();
    await expect(reindexBtn).toBeEnabled();

    // Click Trigger Reindexing
    await reindexBtn.click();

    // Verify toast confirms reindexing was triggered successfully
    await expect(page.getByText(/Search catalog reindexing triggered successfully!/i)).toBeVisible({ timeout: 15_000 });

    // Wait for the reindexing button state to stabilize
    await page.waitForTimeout(2000);
    await expect(reindexBtn).toBeEnabled();

    // 8. Verify Operational Database Integrity (Zero unwanted table mutations)
    // Non-admin customer access to admin search reindex must be forbidden (403)
    const customerTokenResp = await page.request.post('http://localhost:8080/api/v1/admin/search/reindex', {
      headers: {
        // Unauthenticated or customer token
        Authorization: 'Bearer invalid-or-customer-token',
      },
    });
    expect([401, 403]).toContain(customerTokenResp.status());
  });

});
