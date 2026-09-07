import { t, type Locale, type Market } from '@travel/i18n';

/**
 * Băng thông báo **thường trực** khi market khác mặc định của locale.
 *
 * Không phải toast, không tự tắt, không đóng được: khách đang xem giá của một
 * thị trường khác với thị trường mà ngôn ngữ họ đọc gợi ý, và nhầm giá là lỗi
 * đắt nhất mà giao diện gây ra được — docs/02 mục 5.3.
 */
export function MarketBanner({ locale, market }: { locale: Locale; market: Market }) {
  return (
    <p className="market-banner" role="status">
      {t(locale, 'market.banner', { market: t(locale, `market.${market}`) })}
    </p>
  );
}
