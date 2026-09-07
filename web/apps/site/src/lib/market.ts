import { cookies } from 'next/headers';
import { defaultMarketFor, isMarket, type Locale, type Market } from '@travel/i18n';

export { marketSegment } from './market-segment';

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
