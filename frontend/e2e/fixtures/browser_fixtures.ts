import { test as base, expect, Page } from '@playwright/test';

/**
 * PVK Cinemas — P14 Browser Test Fixture Scaffolding
 * Provides shared browser contexts and authentication helpers.
 */

export interface TestUserContext {
  role: string;
  email: string;
}

const TEST_CREDENTIALS = {
  customer: {
    email: 'customer@pvkcinemas.com',
    password: 'Password123!',
  },
  manager_a: {
    email: 'manager@pvkcinemas.com',
    password: 'Password123!',
  },
  manager_b: {
    email: 'manager@pvkcinemas.com',
    password: 'Password123!',
  },
  super_admin: {
    email: 'admin@pvkcinemas.com',
    password: 'Password123!',
  },
};

/* eslint-disable react-hooks/rules-of-hooks */
export const test = base.extend<{
  authenticatedPage: (role: 'customer' | 'manager_a' | 'manager_b' | 'super_admin') => Promise<Page>;
}>({
  authenticatedPage: async ({ page, baseURL }, use) => {
    const loginHelper = async (role: 'customer' | 'manager_a' | 'manager_b' | 'super_admin'): Promise<Page> => {
      const creds = TEST_CREDENTIALS[role];
      if (!creds) {
        throw new Error(`Unknown persona role: ${role}`);
      }

      // 1. Obtain JWT token via backend API
      const apiUrl = 'http://localhost:8080/api/v1/auth/login';
      const resp = await page.request.post(apiUrl, {
        data: {
          email: creds.email,
          password: creds.password,
        },
      });

      if (!resp.ok()) {
        throw new Error(`Authentication helper failed for ${role} (${creds.email}): status ${resp.status()}`);
      }

      const body = await resp.json();
      const token = body.token;
      const userObj = {
        userId: body.userId,
        email: body.email,
        firstName: body.firstName,
        lastName: body.lastName,
        role: body.role,
        roles: [body.role],
        permissions: body.permissions || [],
      };

      // 2. Navigate to base URL and inject session into localStorage
      await page.goto(baseURL || 'http://localhost:3000/');
      await page.evaluate(({ token, userObj }) => {
        localStorage.setItem('pvk_token', token);
        localStorage.setItem('pvk_user', JSON.stringify(userObj));
      }, { token, userObj });

      // 3. Reload page to initialize React AuthProvider with injected credentials
      await page.reload();
      await page.waitForLoadState('networkidle');

      return page;
    };

    await use(loginHelper);
  },
});

export { expect };
