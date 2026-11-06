/**
 * Purpose: Vite build/dev configuration. Proxies /api to the Spring Boot backend so the
 * browser sees a single origin (no CORS, and the Content-Range header stays readable).
 */
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const proxy = { '/api': 'http://127.0.0.1:8080' };

export default defineConfig({
  plugins: [react()],
  server: { host: '127.0.0.1', port: 5173, strictPort: true, proxy },
  preview: { host: '127.0.0.1', port: 5173, strictPort: true, proxy },
});
