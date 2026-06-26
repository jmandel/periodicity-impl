import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Relative base so the built site works whether it is served from the repository
// subpath (https://jmandel.github.io/periodicity-impl/) or a custom domain root.
export default defineConfig({
  base: './',
  plugins: [react()],
  build: {
    outDir: 'dist',
    assetsInlineLimit: 0,
  },
});
