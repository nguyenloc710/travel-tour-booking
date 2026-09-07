import type { MetadataRoute } from 'next';
import { locales } from '@travel/i18n';
import { SITE_URL } from '@/lib/site';

/**
 * `robots.txt`.
 *
 * Khai **cả hai** sitemap, một cho mỗi locale (`docs/20` mục 6). Công cụ tìm
 * kiếm không tự đoán ra `/sitemap/vi.xml` từ `/sitemap/da.xml`.
 *
 * `/api/` chặn thu thập: nó là hợp đồng dữ liệu, không phải nội dung đọc được.
 * Để nó mở là mời công cụ tìm kiếm tiêu ngân sách thu thập vào JSON.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/api/'],
    },
    sitemap: locales.map((locale) => `${SITE_URL}/sitemap/${locale}.xml`),
  };
}
