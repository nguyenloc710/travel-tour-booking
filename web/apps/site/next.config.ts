import type { NextConfig } from 'next';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Gốc của workspace pnpm: web/. Hai cấp lên từ web/apps/site/.
const GOC_WORKSPACE = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

const config: NextConfig = {
  // Ba package trong workspace là TypeScript nguồn, không build sẵn.
  transpilePackages: ['@travel/api-client', '@travel/i18n', '@travel/ui'],

  typedRoutes: true,

  // Ảnh Docker chạy `node apps/site/server.js`, không chạy `next start`: bản
  // standalone gói sẵn đúng phần `node_modules` cần tới, nên ảnh cuối không phải
  // mang theo cả workspace pnpm và toàn bộ devDependencies (docs/34 mục 5.1).
  output: 'standalone',

  // Bắt buộc trong workspace pnpm. Mặc định Next lấy thư mục của app làm gốc
  // truy vết tệp, mà ba package `@travel/*` nằm NGOÀI thư mục đó nên bị bỏ sót.
  // Thiếu dòng này thì ảnh vẫn build xanh rồi chết lúc khởi động vì thiếu
  // module — hỏng ở nơi cách xa nguyên nhân nhất có thể.
  outputFileTracingRoot: GOC_WORKSPACE,

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
