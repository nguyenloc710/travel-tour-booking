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

export function duongDanTimTour(locale: Locale, thamSo?: URLSearchParams): Route {
  const truyVan = thamSo?.toString();
  return `/${locale}/${segmentFor('tourFinder', locale)}${truyVan ? `?${truyVan}` : ''}` as Route;
}
