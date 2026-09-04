import type { MetadataRoute } from 'next';
import { defaultMarketFor, isLocale, locales, type Locale, type Market } from '@travel/i18n';
import type { Destination, PostPage, ProductPage } from '@travel/api-client';
import { destinationsApi, postsApi, productsApi, requestScope } from '@/lib/api';
import { SITE_URL } from '@/lib/site';
import {
  duongDanBlog,
  duongDanChiTiet,
  duongDanDiemDen,
  duongDanLienHe,
  duongDanListing,
  duongDanSuKien,
  duongDanTimTour,
} from '@/lib/routes';

/**
 * **Một sitemap cho mỗi locale** — `docs/20` mục 6.
 *
 * `/sitemap/da.xml` và `/sitemap/vi.xml`. Không gộp làm một file, vì hai locale
 * là hai tập URL khác nhau chứ không phải hai bản dịch của cùng một tập: bản ghi
 * chưa dịch **không có mặt** trong sitemap của locale đó.
 *
 * Đó chính là chính sách không-fallback của `docs/02` mục 4, áp vào SEO. Và ở
 * đây nó tự đúng mà không cần điều kiện `if` nào: API đã ẩn sẵn bản ghi chưa
 * dịch, nên chỉ cần gọi API bằng locale tương ứng.
 *
 * **Trang có tham số bộ lọc không vào sitemap.** Chúng đã `noindex, follow`
 * (`docs/20` mục 6) — liệt kê chúng ở đây là tự mâu thuẫn.
 */
export async function generateSitemaps() {
  return locales.map((locale) => ({ id: locale }));
}

export default async function sitemap(props: {
  id: Promise<string>;
}): Promise<MetadataRoute.Sitemap> {
  // Next 16 đổi `id` thành Promise. Ở bản 15 nó là giá trị trần — `await` một
  // giá trị không phải Promise vẫn chạy đúng, nhưng ngược lại thì không.
  const id = await props.id;
  if (!isLocale(id)) {
    return [];
  }
  const locale: Locale = id;
  const market = defaultMarketFor(locale);

  const [sanPham, diemDen, baiViet] = await Promise.all([
    daySanPham(locale, market),
    dayDiemDen(locale, market),
    dayBaiViet(locale, market),
  ]);

  const gocTrang: MetadataRoute.Sitemap = [
    { url: abs(`/${locale}`), changeFrequency: 'weekly', priority: 1 },
    { url: abs(duongDanListing(locale)), changeFrequency: 'daily', priority: 0.9 },
    { url: abs(duongDanDiemDen(locale)), changeFrequency: 'weekly', priority: 0.8 },
    { url: abs(duongDanTimTour(locale)), changeFrequency: 'monthly', priority: 0.6 },
    { url: abs(duongDanBlog(locale)), changeFrequency: 'weekly', priority: 0.5 },
    // Sự kiện đổi theo lịch nhưng KHÔNG có trang chi tiết riêng cho từng buổi —
    // chỉ một URL, và endpoint chỉ trả buổi chưa diễn ra.
    { url: abs(duongDanSuKien(locale)), changeFrequency: 'weekly', priority: 0.4 },
    { url: abs(duongDanLienHe(locale)), changeFrequency: 'yearly', priority: 0.4 },
  ];

  return [
    ...gocTrang,
    ...sanPham.map((slug) => ({
      url: abs(duongDanChiTiet(locale, slug)),
      changeFrequency: 'weekly' as const,
      priority: 0.8,
    })),
    ...diemDen.map((slug) => ({
      url: abs(duongDanDiemDen(locale, slug)),
      changeFrequency: 'monthly' as const,
      priority: 0.6,
    })),
    ...baiViet.map((slug) => ({
      url: abs(duongDanBlog(locale, slug)),
      // Bài viết là nội dung đọc một lần rồi thôi — sửa hiếm, nên đừng bảo công
      // cụ tìm kiếm quay lại hằng tuần cho một trang không đổi.
      changeFrequency: 'yearly' as const,
      priority: 0.4,
    })),
  ];
}

/**
 * Mọi slug sản phẩm của locale này, lấy hết qua nhiều trang.
 *
 * Đi vòng lặp thay vì xin một trang thật to: `size` có trần trong hợp đồng
 * (`docs/13` mục 6), và một tham số vượt trần trả `400` chứ không trả thêm dữ
 * liệu. Trần vòng lặp là chốt an toàn — API hỏng kiểu trả mãi một trang thì
 * vòng này vẫn dừng, chứ không treo cả lần dựng sitemap.
 */
async function daySanPham(locale: Locale, market: Market) {
  const slugs: string[] = [];
  const size = 60;

  for (let trang = 0; trang < 50; trang++) {
    let ket_qua: ProductPage;
    try {
      ket_qua = await productsApi().listProducts({
        ...requestScope(market, locale),
        page: trang,
        size,
      });
    } catch (loi) {
      // Sitemap thiếu vài URL còn hơn sitemap không dựng được: lỗi ở trang thứ
      // ba không được xoá sạch hai trang đầu.
      console.error('[sitemap] listProducts thất bại', loi);
      break;
    }
    slugs.push(...ket_qua.items.map((sp) => sp.slug));
    if (trang + 1 >= ket_qua.totalPages) {
      break;
    }
  }
  return slugs;
}

async function dayDiemDen(locale: Locale, market: Market) {
  let ds: Destination[];
  try {
    ds = await destinationsApi().listDestinations(requestScope(market, locale));
  } catch (loi) {
    console.error('[sitemap] listDestinations thất bại', loi);
    return [];
  }
  return ds.map((d) => d.slug);
}

/**
 * Slug mọi bài viết của locale này.
 *
 * Cùng khuôn với sản phẩm: đi vòng lặp thay vì xin một trang thật to, và trần
 * vòng lặp là chốt an toàn. Bài chưa dịch **không có mặt** — backend đã ẩn sẵn,
 * nên sitemap `vi` ngắn hơn sitemap `da` mà không cần một điều kiện `if` nào.
 */
async function dayBaiViet(locale: Locale, market: Market) {
  const slugs: string[] = [];
  const size = 60;

  for (let trang = 0; trang < 50; trang++) {
    let ket_qua: PostPage;
    try {
      ket_qua = await postsApi().listPosts({
        ...requestScope(market, locale),
        page: trang,
        size,
      });
    } catch (loi) {
      console.error('[sitemap] listPosts thất bại', loi);
      break;
    }
    slugs.push(...ket_qua.items.map((b) => b.slug));
    if (trang + 1 >= ket_qua.totalPages) {
      break;
    }
  }
  return slugs;
}

function abs(duongDan: string): string {
  return `${SITE_URL}${duongDan}`;
}
