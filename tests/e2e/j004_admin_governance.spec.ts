import { test, expect } from './fixtures/browser_fixtures';

/**
 * PVK Cinemas — J004: Super Admin Catalogue & Audit Governance E2E Journey
 * Authoritative Basis: Test Strategy §16, SRS FR-010–FR-012, FR-020–FR-023, FR-050–FR-055, FR-110–FR-112.
 *
 * Flow: [UI-15 Admin Dashboard] -> [UI-19 Movie Management] -> [Create Movie] -> [UI-20 Audit Logs] -> [Verify Event] -> [UI-16 User Management] -> [Toggle User Status]
 * Invariants:
 * 1. Super Admin has exclusive governance access.
 * 2. Movie catalogue creations synchronously append immutable AUDIT_LOG records.
 * 3. User activation status can be toggled and reflected in real-time.
 */

test.describe('J004: Super Admin Catalogue & Audit Governance', () => {

  test('J004-01: Super Admin creates movie, verifies synchronous audit trail, and manages user status', async ({ authenticatedPage, page }) => {
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
    await expect(page.getByText(/Super Admin Control Center/i)).toBeVisible();
    await expect(page.getByText(/Platform Administration/i)).toBeVisible();

    // 4. Navigate to Movie Catalogue (UI-19)
    const movieCatalogueCard = page.locator('.card:has-text("Movie Catalogue")');
    await expect(movieCatalogueCard).toBeVisible();
    await movieCatalogueCard.click();
    await page.waitForLoadState('networkidle');

    const movieManagementView = page.locator('[data-testid="movie-management-view"]');
    await expect(movieManagementView).toBeVisible();
    await expect(page.locator('h1')).toContainText('Movie Catalogue CRUD');

    // 5. Open Add Movie Modal
    const addMovieBtn = page.locator('button:has-text("Add Movie to Catalogue")');
    await expect(addMovieBtn).toBeVisible();
    await addMovieBtn.click();

    // Verify modal opened
    await expect(page.locator('h3:has-text("Add Movie to Global Catalogue")')).toBeVisible();

    // Fill new movie details
    const uniqueTitle = `E2E Film ${Date.now().toString().slice(-6)}`;
    await page.locator('#m-title').fill(uniqueTitle);
    await page.locator('#m-cert').selectOption({ value: '2' });
    await page.locator('#m-runtime').fill('135');
    await page.locator('#m-release').fill('2026-11-20');
    await page.locator('#m-status').selectOption({ value: 'AIRING' });
    await page.locator('#m-synopsis').fill('High-stakes corporate intrigue in deep space exploration.');

    // Submit movie creation
    const submitBtn = page.locator('button[type="submit"]:has-text("Create Movie")');
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();
    await page.waitForLoadState('networkidle');

    // Verify success toast
    await expect(page.getByText(/New movie added to catalogue!/i)).toBeVisible({ timeout: 10_000 });

    // 6. Navigate to Audit Logs (UI-20)
    await page.locator('button:has-text("Back to Admin Dashboard")').click();
    await page.waitForLoadState('networkidle');
    await expect(adminDashboard).toBeVisible();

    const auditTrailCard = page.locator('.card:has-text("Audit Trail Logs")');
    await expect(auditTrailCard).toBeVisible();
    await auditTrailCard.click();
    await page.waitForLoadState('networkidle');

    const auditLogsView = page.locator('[data-testid="audit-logs-view"]');
    await expect(auditLogsView).toBeVisible();
    await expect(page.locator('h1')).toContainText('Immutable Audit Trail');

    // Verify audit log records are present
    const auditRows = page.locator('table.data-table tbody tr');
    await expect(auditRows.first()).toBeVisible({ timeout: 10_000 });
    const rowCount = await auditRows.count();
    expect(rowCount).toBeGreaterThan(0);

    // Verify the movie creation event is recorded at the top of the audit trail with the unique title
    const topAuditRow = page.locator('table.data-table tbody tr').first();
    await expect(topAuditRow).toContainText('MOVIE_CREATE');
    await expect(topAuditRow).toContainText(uniqueTitle);

    // 7. Navigate to User Management (UI-16)
    await page.locator('button:has-text("Back to Admin Dashboard")').click();
    await page.waitForLoadState('networkidle');
    await expect(adminDashboard).toBeVisible();

    const userManagementCard = page.locator('.card:has-text("User Management")');
    await expect(userManagementCard).toBeVisible();
    await userManagementCard.click();
    await page.waitForLoadState('networkidle');

    const userManagementView = page.locator('[data-testid="user-management-view"]');
    await expect(userManagementView).toBeVisible();
    await expect(page.locator('h1')).toContainText('User Management');

    // Locate target user row (find first row with action button)
    const firstActionBtn = page.locator('table.data-table tbody tr td button').first();
    await expect(firstActionBtn).toBeVisible({ timeout: 10_000 });

    const initialBtnText = await firstActionBtn.innerText();
    expect(['Suspend', 'Activate']).toContain(initialBtnText.trim());

    // Toggle user status
    await firstActionBtn.click();
    await page.waitForLoadState('networkidle');

    // Verify toast confirms status change
    await expect(page.getByText(/marked as (ACTIVE|SUSPENDED)/i).first()).toBeVisible({ timeout: 10_000 });

    // Toggle back to restore original state
    await page.waitForTimeout(500);
    const updatedActionBtn = page.locator('table.data-table tbody tr td button').first();
    await updatedActionBtn.click();
    await page.waitForLoadState('networkidle');
    await expect(page.getByText(/marked as (ACTIVE|SUSPENDED)/i).last()).toBeVisible({ timeout: 10_000 });
  });

});
