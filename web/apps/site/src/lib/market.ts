import { cookies } from 'next/headers';
import { defaultMarketFor, isMarket, type Locale, type Market } from '@travel/i18n';

/**
 * Market đang áp dụng cho request này.
 *
 * Thứ tự: cookie `market` do khách chọn → mặc định của locale. **Không bao giờ
 * suy market từ locale rồi coi đó là lựa chọn của khách** — hai thứ độc lập,
 * và khách Việt sống ở Đan Mạch mua ở `DK` nhưng đọc `vi`.
 */
export async function resolveMarket(locale: Locale): Promise<{
  market: Market;
  laMacDinh: boolean;
}> {
  const kho = await cookies();
  const daChon = kho.get('market')?.value;
  const macDinh = defaultMarketFor(locale);

  if (isMarket(daChon)) {
    return { market: daChon, laMacDinh: daChon === macDinh };
  }
  return { market: macDinh, laMacDinh: true };
}

/**
 * Market chữ thường khi đi vào đường dẫn API — api/CLAUDE.md mục 10.
 *
 * Kiểu trả về là union chữ thường chứ không phải `string`, để khớp thẳng với
 * enum sinh từ spec: thêm một market vào `openapi.yaml` mà quên thêm ở đây là
 * lỗi biên dịch.
 */
export function marketSegment(market: Market): Lowercase<Market> {
  return market.toLowerCase() as Lowercase<Market>;
}
