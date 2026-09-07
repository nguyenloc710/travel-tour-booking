import type { Route } from 'next';
import { segmentFor, type Locale } from '@travel/i18n';

/**
 * Đoạn đường dẫn của trang danh sách sản phẩm, theo locale.
 *
 * `da` dùng `/da/rejser/…`, `vi` dùng `/vi/tour/…`. **Đoạn tiếng Đan không mở
 * được ở trang tiếng Việt** — đó là chủ ý, không phải thiếu sót: URL của mỗi
 * ngôn ngữ phải đọc được bằng chính ngôn ngữ đó (docs/02 mục 5.1).
 */
export function productsSegment(locale: Locale): string {
  return segmentFor('products', locale);
}

export function laProductsSegment(locale: Locale, doan: string): boolean {
  return doan === productsSegment(locale);
}

export function destinationsSegment(locale: Locale): string {
  return segmentFor('destinations', locale);
}

export function laDestinationsSegment(locale: Locale, doan: string): boolean {
  return doan === destinationsSegment(locale);
}

export function laTourFinderSegment(locale: Locale, doan: string): boolean {
  return doan === segmentFor('tourFinder', locale);
}

export function laBookingSegment(locale: Locale, doan: string): boolean {
  return doan === segmentFor('booking', locale);
}

export function laConfirmationSegment(locale: Locale, doan: string): boolean {
  return doan === segmentFor('confirmation', locale);
}

export function laBlogSegment(locale: Locale, doan: string): boolean {
  return doan === segmentFor('blog', locale);
}

export function laEventsSegment(locale: Locale, doan: string): boolean {
  return doan === segmentFor('events', locale);
}

export function laContactSegment(locale: Locale, doan: string): boolean {
  return doan === segmentFor('contact', locale);
}

/**
 * Đường dẫn có kiểu.
 *
 * `typedRoutes` của Next kiểm `href` lúc biên dịch, nhưng đường dẫn dựng động
 * từ locale và slug thì nó không suy ra được — nên ép kiểu ở **một chỗ duy nhất
 * này** thay vì rải `as Route` khắp component.
 */
export function duongDanListing(locale: Locale, thamSo?: URLSearchParams): Route {
  const truyVan = thamSo?.toString();
  return `/${locale}/${productsSegment(locale)}${truyVan ? `?${truyVan}` : ''}` as Route;
}

export function duongDanChiTiet(locale: Locale, slug: string): Route {
  return `/${locale}/${productsSegment(locale)}/${slug}` as Route;
}

export function duongDanDiemDen(locale: Locale, slug?: string): Route {
  const goc = `/${locale}/${destinationsSegment(locale)}`;
  return (slug ? `${goc}/${slug}` : goc) as Route;
}

/**
 * Bốn bước đặt tour là **bốn URL**, không phải bốn tab ẩn hiện bằng JavaScript:
 * khách bấm Back là chuyện chắc chắn xảy ra, và mất hết dữ liệu vì Back là cách
 * nhanh nhất để mất một đơn (`docs/23` mục 2).
 *
 * **Không đưa dữ liệu hành khách vào URL.** Tên và ngày sinh là dữ liệu cá nhân,
 * và URL đi thẳng vào nhật ký máy chủ (`docs/31` mục 7). Bước 4 giữ chúng trong
 * bộ nhớ trang và gửi thẳng lên API.
 */
export function duongDanDatTour(
  locale: Locale,
  slug: string,
  thamSo?: URLSearchParams,
): Route {
  const truyVan = thamSo?.toString();
  return `/${locale}/${segmentFor('booking', locale)}/${slug}${truyVan ? `?${truyVan}` : ''}` as Route;
}

export function duongDanXacNhan(locale: Locale, reference: string, email: string): Route {
  const q = new URLSearchParams({ email });
  return `/${locale}/${segmentFor('confirmation', locale)}/${reference}?${q}` as Route;
}

/**
 * Blog (R9). Danh sách có bộ lọc thẻ trong truy vấn; chi tiết thì không.
 *
 * Đoạn đường dẫn trùng nhau ở hai locale (`blog` cả `da` lẫn `vi`) — đó là ngoại
 * lệ duy nhất của bảng `pathnames`, và nó không phá luật nào: luật là URL phải
 * đọc được bằng chính ngôn ngữ đó, mà "blog" đọc được ở cả hai.
 */
export function duongDanBlog(
  locale: Locale,
  slug?: string,
  thamSo?: URLSearchParams,
): Route {
  const goc = `/${locale}/${segmentFor('blog', locale)}`;
  const truyVan = thamSo?.toString();
  return `${slug ? `${goc}/${slug}` : goc}${truyVan ? `?${truyVan}` : ''}` as Route;
}

export function duongDanSuKien(locale: Locale): Route {
  return `/${locale}/${segmentFor('events', locale)}` as Route;
}

export function duongDanLienHe(locale: Locale): Route {
  return `/${locale}/${segmentFor('contact', locale)}` as Route;
}

export function duongDanTimTour(locale: Locale, thamSo?: URLSearchParams): Route {
  const truyVan = thamSo?.toString();
  return `/${locale}/${segmentFor('tourFinder', locale)}${truyVan ? `?${truyVan}` : ''}` as Route;
}
