import { defineConfig } from 'vite';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDirectory = fileURLToPath(new URL('.', import.meta.url));

// Explicit multi-page build: Login Portal and Operations Application stay
// independently addressable instead of toggling one HTML page with CSS.
export default defineConfig({
  server: {
    host: '0.0.0.0',
    // Required for sandbox/remote preview hosts; production deployment should
    // supply its own host policy at the reverse proxy boundary.
    allowedHosts: true,
  },
  build: {
    rollupOptions: {
      input: {
        login: resolve(rootDirectory, 'index.html'),
        app: resolve(rootDirectory, 'app.html'),
      },
    },
  },
});
