import { expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { test } from '../e2e/fixtures/browser_fixtures';

const PUBLIC_SCREENS = [
  { id: 'UI-01', name: 'Home', view: 'home' },
  { id: 'UI-02', name: 'Movie Listing', view: 'movies' },
  { id: 'UI-03', name: 'Movie Details', view: 'movie-details', param: 1 },
  { id: 'UI-04', name: 'Search Results', view: 'search', param: 'Inception' },
  { id: 'UI-05', name: 'City/Theatres', view: 'theatres' },
  { id: 'UI-06', name: 'Theatre Details', view: 'theatre-details', param: 1 },
  { id: 'UI-07', name: 'Show Details', view: 'show-details', param: 1 },
  { id: 'UI-08', name: 'Seat Availability', view: 'show-seats', param: 1 },
  { id: 'UI-09', name: 'Authentication', view: 'login' },
];

const MANAGER_SCREENS = [
  { id: 'UI-11', name: 'Manager Dashboard', view: 'manager-dashboard' },
  { id: 'UI-12', name: 'Screen Management', view: 'manager-screens', param: 1 },
  { id: 'UI-13', name: 'Seat Management', view: 'manager-seats', param: 1 },
  { id: 'UI-14', name: 'Show Management', view: 'manager-shows', param: 1 },
];

const ADMIN_SCREENS = [
  { id: 'UI-15', name: 'Admin Dashboard', view: 'admin-dashboard' },
  { id: 'UI-16', name: 'User Management', view: 'admin-users' },
  { id: 'UI-17', name: 'Role & Permissions', view: 'admin-roles' },
  { id: 'UI-18', name: 'City/Theatre Management', view: 'admin-cities-theatres' },
  { id: 'UI-19', name: 'Movie Management', view: 'admin-movies' },
  { id: 'UI-20', name: 'Audit Logs', view: 'admin-audit' },
  { id: 'UI-21', name: 'Search Administration', view: 'admin-search' },
];

test.describe('P14-F: Automated WCAG 2.1 AA Audit (All 21 Screens)', () => {
  test('Audit Public Screens (UI-01 through UI-09)', async ({ page }) => {
    await page.goto('http://localhost:3000/');
    await page.waitForLoadState('networkidle');

    for (const screen of PUBLIC_SCREENS) {
      await page.evaluate(({ view, param }) => {
        (window as any).__pvk_navigate(view, param);
      }, { view: screen.view, param: screen.param });

      await page.waitForTimeout(400);

      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();

      console.log(`[${screen.id}] ${screen.name}: ${results.violations.length} violations`);
      expect(results.violations, `Screen ${screen.id} (${screen.name}) has accessibility violations: ${JSON.stringify(results.violations.map(v => ({ id: v.id, help: v.help, nodes: v.nodes.length })))}`).toEqual([]);
    }
  });

  test('Audit Customer Profile (UI-10)', async ({ authenticatedPage }) => {
    const page = await authenticatedPage('customer');
    await page.waitForLoadState('networkidle');

    await page.evaluate(() => {
      (window as any).__pvk_navigate('profile');
    });
    await page.waitForTimeout(400);

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    console.log(`[UI-10] Profile: ${results.violations.length} violations`);
    expect(results.violations, `Screen UI-10 has accessibility violations`).toEqual([]);
  });

  test('Audit Manager Screens (UI-11 through UI-14)', async ({ authenticatedPage }) => {
    const page = await authenticatedPage('manager_a');
    await page.waitForLoadState('networkidle');

    for (const screen of MANAGER_SCREENS) {
      await page.evaluate(({ view, param }) => {
        (window as any).__pvk_navigate(view, param);
      }, { view: screen.view, param: screen.param });

      await page.waitForTimeout(400);

      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();

      console.log(`[${screen.id}] ${screen.name}: ${results.violations.length} violations`);
      expect(results.violations, `Screen ${screen.id} has accessibility violations`).toEqual([]);
    }
  });

  test('Audit Super Admin Screens (UI-15 through UI-21)', async ({ authenticatedPage }) => {
    const page = await authenticatedPage('super_admin');
    await page.waitForLoadState('networkidle');

    for (const screen of ADMIN_SCREENS) {
      await page.evaluate(({ view, param }) => {
        (window as any).__pvk_navigate(view, param);
      }, { view: screen.view, param: screen.param });

      await page.waitForTimeout(400);

      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();

      console.log(`[${screen.id}] ${screen.name}: ${results.violations.length} violations`);
      expect(results.violations, `Screen ${screen.id} has accessibility violations`).toEqual([]);
    }
  });
});
