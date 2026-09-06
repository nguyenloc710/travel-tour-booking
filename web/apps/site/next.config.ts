import type { NextConfig } from 'next';

const config: NextConfig = {
  // Ba package trong workspace là TypeScript nguồn, không build sẵn.
  transpilePackages: ['@travel/api-client', '@travel/i18n', '@travel/ui'],

  typedRoutes: true,

  images: {
    // Ảnh bộ đến từ kho đối tượng (ADR-011), không từ `public/`. Địa chỉ đầy đủ
    // do API ghép từ `media_asset.path` và cấu hình của nó; phía này chỉ cần
    // biết host nào được phép.
    //
    // Hôm nay chưa dùng tới: mọi `<Image>` trong dự án đang đặt `unoptimized`,
    // và ảnh không đi qua bộ tối ưu thì `next/image` không kiểm host. Khai sẵn
    // vì ngày bỏ `unoptimized` — quyết định còn để ngỏ ở `24` mục 7.1 — thiếu
    // dòng này thì mọi ảnh bộ hỏng, và nó hỏng im lặng chứ không báo lỗi.
    remotePatterns: [
      {
        protocol: (process.env.NEXT_PUBLIC_STORAGE_PROTOCOL ?? 'http') as 'http' | 'https',
        hostname: process.env.NEXT_PUBLIC_STORAGE_HOST ?? 'localhost',
        port: process.env.NEXT_PUBLIC_STORAGE_PORT ?? '9000',
        pathname: '/**',
      },
    ],
  },

  // Locale nằm ở đoạn đầu đường dẫn nên KHÔNG dùng i18n routing dựng sẵn của
  // Next: nó gắn locale với domain và cookie theo cách riêng, còn dự án này
  // tách locale (URL) khỏi market (cookie) — docs/02 mục 5.3.
};

export default config;
