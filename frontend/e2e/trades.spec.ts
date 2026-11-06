/**
 * Purpose: smoke test proving a SECOND schema (trade.json) was generated into a working
 * list view too (15,000 seeded trades) without any hand-written UI code.
 */
import { test, expect } from '@playwright/test';

test('generated Trades list shows 15,000 rows with paging', async ({ page }) => {
  await page.goto('/#/trades');
  await expect(page.getByTestId('page-info')).toHaveText('1-25 of 15000');
  await expect(page.getByTestId('row')).toHaveCount(25);
  await page.getByRole('button', { name: 'Next page' }).click();
  await expect(page.getByTestId('page-info')).toHaveText('26-50 of 15000');
});
