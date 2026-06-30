/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Minimal self-contained server bundle for the Docker image (node server.js).
  output: 'standalone',
  // Served behind nginx at the site root. Images come from the backend /uploads
  // and /covers paths (same origin in prod), so the default loader is fine.
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: '**' },
      { protocol: 'http', hostname: 'localhost' },
    ],
  },
  // Don't fail the production build on lint/type issues during the migration;
  // CI runs them separately. (Tighten once the migration is complete.)
  eslint: { ignoreDuringBuilds: true },
};

export default nextConfig;
