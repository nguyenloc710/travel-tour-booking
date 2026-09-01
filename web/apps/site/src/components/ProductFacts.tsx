import { t, isLocale, type Locale } from '@travel/i18n';
import { formatDate, formatNumber } from '@travel/ui';
import type { ProductDetail } from '@travel/api-client';

/**
 * Khối "thông tin chuyến đi" — mỗi loại sản phẩm một bộ dữ kiện khác nhau.
 *
 * `switch` trên `productType` của kiểu tổng sinh từ spec: thêm loại thứ bảy vào
 * `openapi.yaml` mà quên chỗ này thì **`pnpm typecheck` đỏ** ở nhánh `default`,
 * chứ không phải trang thiếu mất một khối mà không ai để ý. Đó là toàn bộ lý do
 * dùng `discriminator` trong spec (docs/13 mục 9.1).
 */
export function ProductFacts({ product, locale }: { product: ProductDetail; locale: Locale }) {
  return (
    <section className="facts">
      <h2>{t(locale, 'detail.facts')}</h2>
      <ul className="facts__list">
        {dongDuLieu(product, locale).map((dong) => (
          <li key={dong}>{dong}</li>
        ))}
      </ul>

      {product.productType === 'PRIVATE_TOUR' && (
        // Tour riêng KHÔNG đặt trực tiếp được — CTA là yêu cầu báo giá.
        // Nút "Đặt ngay" ở đây là hứa một thứ hệ thống không làm được.
        <p className="facts__note">{t(locale, 'detail.private.note')}</p>
      )}
    </section>
  );
}

function dongDuLieu(product: ProductDetail, locale: Locale): string[] {
  const so = (n: number) => formatNumber(n, locale);

  switch (product.productType) {
    case 'GROUP_TOUR':
      return [
        t(locale, 'detail.groupTour.pax', { min: so(product.minPax), max: so(product.maxPax) }),
        t(locale, 'detail.groupTour.guaranteed', { count: so(product.guaranteedThreshold) }),
        // Ngôn ngữ của TRƯỞNG ĐOÀN, không phải locale khách đang đọc: khách
        // Việt sống ở Đan Mạch đọc `vi` nhưng đi đoàn nói tiếng Đan.
        t(locale, 'detail.groupTour.leader', { language: tenNgonNgu(product.tourLeaderLanguage, locale) }),
        t(locale, 'detail.groupTour.fitness', { level: so(product.fitnessLevel) }),
      ];

    case 'INDIVIDUAL_PACKAGE':
      return [
        t(locale, 'detail.individual.minPartySize', { count: so(product.minPartySize) }),
        t(locale, 'detail.individual.window', { days: so(product.flexibleDateWindowDays) }),
      ];

    case 'PRIVATE_TOUR':
      return [
        t(locale, 'detail.private.leadTime', { days: so(product.leadTimeDays) }),
        t(locale, 'detail.private.quoteValid', { days: so(product.quoteValidDays) }),
      ];

    case 'CRUISE':
      return [
        t(locale, 'detail.cruise.ship', { name: product.shipName }),
        t(locale, 'detail.cruise.ports', { count: so(product.portCount) }),
      ];

    case 'COMBO':
      return [
        t(locale, 'detail.combo.nights', { count: so(product.nights) }),
        t(locale, 'detail.combo.valid', {
          from: formatDate(product.validFrom, locale),
          to: formatDate(product.validTo, locale),
        }),
      ];

    case 'DAY_TOUR':
      return [
        t(locale, 'detail.dayTour.duration', { hours: so(product.durationHours) }),
        t(locale, 'detail.dayTour.cutoff', { hours: so(product.cutoffHours) }),
      ];
  }
}

/** Tên ngôn ngữ viết bằng tiếng của khách: "dansk" với `da`, "tiếng Đan Mạch" với `vi`. */
function tenNgonNgu(ma: string, locale: Locale): string {
  return isLocale(ma) ? t(locale, `language.${ma}`) : ma;
}
