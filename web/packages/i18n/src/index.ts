import da from '../messages/da.json' with { type: 'json' };
import vi from '../messages/vi.json' with { type: 'json' };

export { pathnames, segmentFor } from './pathnames';

/** Ngôn ngữ nguồn — ADR-004. Nội dung viết bằng `da` trước, dịch sang `vi` sau. */
export const SOURCE_LOCALE = 'da' as const;

export const locales = ['da', 'vi'] as const;
export type Locale = (typeof locales)[number];

export const markets = ['DK', 'VN'] as const;
export type Market = (typeof markets)[number];

const catalogs: Record<Locale, Record<string, string>> = { da, vi };

export function isLocale(value: string | undefined): value is Locale {
  return value !== undefined && (locales as readonly string[]).includes(value);
}

export function isMarket(value: string | undefined): value is Market {
  return value !== undefined && (markets as readonly string[]).includes(value);
}

/**
 * Market MẶC ĐỊNH của một locale. Chỉ là mặc định — khách ghi đè bằng cookie.
 *
 * `Market` quyết định khách **mua** gì, `Locale` quyết định khách **đọc** bằng
 * tiếng gì. Một khách Việt sống ở Đan Mạch mua ở `DK` nhưng đọc `vi`. Đừng dùng
 * hàm này ở chỗ cần market thật — dùng giá trị đã giải từ cookie.
 */
export function defaultMarketFor(locale: Locale): Market {
  return locale === 'da' ? 'DK' : 'VN';
}

/**
 * Tra chuỗi giao diện.
 *
 * Chuỗi giao diện **được phép** fallback về `da`, có ghi log — thiếu một nhãn
 * nút không phá vỡ lòng tin của khách. `pnpm i18n:check` biến thiếu khoá thành
 * lỗi build nên fallback chỉ là lưới an toàn lúc chạy.
 *
 * Nội dung bán hàng thì NGƯỢC LẠI: thiếu bản dịch là ẩn hoàn toàn, và việc đó
 * do backend làm bằng `INNER JOIN`. Đừng bao giờ dùng hàm này để lấp nội dung.
 */
export function t(
  locale: Locale,
  key: string,
  params?: Record<string, string | number>,
): string {
  const chuoi = catalogs[locale]?.[key] ?? fallback(locale, key);
  if (!params) return chuoi;
  return chuoi.replace(/\{(\w+)\}/g, (khop, ten: string) =>
    ten in params ? String(params[ten]) : khop,
  );
}

function fallback(locale: Locale, key: string): string {
  const nguon = catalogs[SOURCE_LOCALE][key];
  if (nguon !== undefined) {
    if (locale !== SOURCE_LOCALE) {
      console.warn(`[i18n] thiếu khoá "${key}" cho locale "${locale}", dùng bản ${SOURCE_LOCALE}`);
    }
    return nguon;
  }
  console.error(`[i18n] khoá "${key}" không tồn tại ở bất kỳ locale nào`);
  return key;
}

/** Ngôn ngữ nào dùng cho `Accept-Language` khi gọi API. */
export function acceptLanguage(locale: Locale): string {
  return locale;
}
