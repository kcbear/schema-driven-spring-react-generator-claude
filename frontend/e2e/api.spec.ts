/**
 * Purpose: API-level tests (through the Vite proxy) of the generated Spring controllers:
 * react-admin Content-Range paging contract and the seeded record counts.
 */
import { test, expect } from '@playwright/test';

test('GET /api/instruments returns a page and total 12000 in Content-Range', async ({ request }) => {
  const res = await request.get('/api/instruments', {
    params: { sort: '["symbol","ASC"]', range: '[0,9]' },
  });
  expect(res.ok()).toBeTruthy();
  expect(res.headers()['content-range']).toBe('instruments 0-9/12000');
  expect(await res.json()).toHaveLength(10);
});

test('GET /api/trades reports total 15000', async ({ request }) => {
  const res = await request.get('/api/trades', { params: { range: '[0,4]' } });
  expect(res.headers()['content-range']).toBe('trades 0-4/15000');
});
