import type { NextConfig } from 'next';

const config: NextConfig = {
  // Ba package trong workspace là TypeScript nguồn, không build sẵn.
  transpilePackages: ['@travel/api-client', '@travel/i18n', '@travel/ui'],

  typedRoutes: true,

  // Locale nằm ở đoạn đầu đường dẫn nên KHÔNG dùng i18n routing dựng sẵn của
  // Next: nó gắn locale với domain và cookie theo cách riêng, còn dự án này
  // tách locale (URL) khỏi market (cookie) — docs/02 mục 5.3.
};

export default config;
