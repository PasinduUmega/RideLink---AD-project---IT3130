import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

const services = {
  '/svc/account': 'http://localhost:8081',
  '/svc/driver-vehicle': 'http://localhost:8082',
  '/svc/ride-management': 'http://localhost:8083',
  '/svc/fare-payment': 'http://localhost:8084',
};

const proxy = Object.fromEntries(
  Object.entries(services).map(([prefix, target]) => [
    prefix,
    { target, changeOrigin: true, rewrite: (path) => path.replace(prefix, '') },
  ]),
);

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173, proxy },
});
