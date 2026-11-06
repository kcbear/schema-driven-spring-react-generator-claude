/**
 * Purpose: end-to-end tests of the generated Instruments list (12,000 seeded stocks/ETFs):
 * first page, paging forward/last, page size, server-side sorting, enum filter and free-text search.
 */
import { test, expect } from '@playwright/test';

const TOTAL = 12000;

test.describe('Instruments list at scale', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/instruments');
    await expect(page.getByTestId('page-info')).toHaveText('1-25 of ' + TOTAL);
  });

  test('renders the first page of 25 rows out of 12,000', async ({ page }) => {
    await expect(page.getByTestId('row')).toHaveCount(25);
    await expect(page.getByRole('columnheader', { name: 'Symbol' })).toBeVisible();
  });

  test('next page shows different rows', async ({ page }) => {
    const firstBefore = await page.getByTestId('cell-symbol').first().textContent();
    await page.getByRole('button', { name: 'Next page' }).click();
    await expect(page.getByTestId('page-info')).toHaveText('26-50 of ' + TOTAL);
    await expect(page.getByTestId('cell-symbol').first()).not.toHaveText(firstBefore ?? '');
  });

  test('last page is reachable', async ({ page }) => {
    await page.getByRole('button', { name: 'Last page' }).click();
    await expect(page.getByTestId('page-info')).toHaveText('11976-12000 of ' + TOTAL);
    await expect(page.getByTestId('row')).toHaveCount(25);
    await expect(page.getByRole('button', { name: 'Next page' })).toBeDisabled();
  });

  test('page size 100 loads 100 rows', async ({ page }) => {
    await page.getByTestId('page-size').selectOption('100');
    await expect(page.getByTestId('page-info')).toHaveText('1-100 of ' + TOTAL);
    await expect(page.getByTestId('row')).toHaveCount(100);
  });

  test('clicking the Symbol header sorts descending on the server', async ({ page }) => {
    await page.getByRole('columnheader', { name: 'Symbol' }).getByRole('button').click();
    await expect(async () => {
      const symbols = await page.getByTestId('cell-symbol').allTextContents();
      expect(symbols).toHaveLength(25);
      expect(symbols).toEqual([...symbols].sort().reverse());
    }).toPass();
  });

  test('filters by instrument type ETF', async ({ page }) => {
    const filter = encodeURIComponent(JSON.stringify({ instrumentType: 'ETF' }));
    await page.goto('/#/instruments?filter=' + filter);
    await expect(page.getByTestId('page-info')).not.toHaveText(new RegExp('of ' + TOTAL + '$'));
    await expect(async () => {
      const types = await page.getByTestId('cell-instrumentType').allTextContents();
      expect(types.length).toBeGreaterThan(0);
      expect(new Set(types)).toEqual(new Set(['ETF']));
    }).toPass();
  });

  test('free-text search narrows results to one symbol', async ({ page }) => {
    const symbol = (await page.getByTestId('cell-symbol').first().textContent()) ?? '';
    await page.getByLabel('Search').fill(symbol);
    await expect(async () => {
      const symbols = await page.getByTestId('cell-symbol').allTextContents();
      expect(symbols).toContain(symbol);
      expect(symbols.length).toBeLessThan(25);
    }).toPass();
  });
});
