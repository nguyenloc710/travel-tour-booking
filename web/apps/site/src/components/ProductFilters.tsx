import { t, type Locale } from '@travel/i18n';
import { ProductSort, ProductType } from '@travel/api-client';
import { regionsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import { productsSegment } from '@/lib/routes';
import type { BoLoc } from '@/lib/filters';

const NHAN_SAP_XEP: Record<string, string> = {
  [ProductSort.Titleasc]: 'products.sort.titleAsc',
  [ProductSort.PriceFromasc]: 'products.sort.priceAsc',
  [ProductSort.PriceFromdesc]: 'products.sort.priceDesc',
  [ProductSort.DurationDaysasc]: 'products.sort.durationAsc',
};

/**
 * Bộ lọc là một `<form method="get">` thuần.
 *
 * Không cần JavaScript: bấm "Xem kết quả" là trình duyệt tự dựng URL có đúng
 * các tham số, tức là trạng thái bộ lọc **luôn** nằm trong URL mà không phải
 * viết code đồng bộ. Khách lớn tuổi dùng trình duyệt cũ vẫn lọc được.
 *
 * Danh sách miền lấy từ API trong phạm vi (market, locale) — miền chưa dịch
 * không xuất hiện, nên bộ lọc không bao giờ chào một lựa chọn ra 0 kết quả vì
 * lý do ngôn ngữ.
 */
export async function ProductFilters({ locale, boLoc }: { locale: Locale; boLoc: BoLoc }) {
  const { market } = await resolveMarket(locale);

  let mien: { slug: string; name: string }[] = [];
  try {
    mien = await regionsApi().listRegions(requestScope(market, locale));
  } catch (loi) {
    // Bộ lọc hỏng thì vẫn hiện listing — chỉ mất ô chọn miền.
    console.error('[api] listRegions cho bộ lọc thất bại', loi);
  }

  return (
    <form className="filters" method="get" action={`/${locale}/${productsSegment(locale)}`}>
      <fieldset>
        <legend>{t(locale, 'products.filter.legend')}</legend>

        <p className="filters__field">
          <label htmlFor="loc-q">{t(locale, 'products.filter.q')}</label>
          <input id="loc-q" name="q" type="search" defaultValue={boLoc.q ?? ''} />
        </p>

        {mien.length > 0 && (
          <p className="filters__field">
            <label htmlFor="loc-region">{t(locale, 'products.filter.region')}</label>
            <select id="loc-region" name="region" defaultValue={boLoc.region ?? ''}>
              <option value="">{t(locale, 'products.filter.all')}</option>
              {mien.map((m) => (
                <option key={m.slug} value={m.slug}>
                  {m.name}
                </option>
              ))}
            </select>
          </p>
        )}

        <p className="filters__field">
          <label htmlFor="loc-type">{t(locale, 'products.filter.type')}</label>
          <select id="loc-type" name="productType" defaultValue={boLoc.productType ?? ''}>
            <option value="">{t(locale, 'products.filter.all')}</option>
            {Object.values(ProductType).map((loai) => (
              <option key={loai} value={loai}>
                {t(locale, `productType.${loai}`)}
              </option>
            ))}
          </select>
        </p>

        <p className="filters__field">
          <label htmlFor="loc-sort">{t(locale, 'products.filter.sort')}</label>
          <select id="loc-sort" name="sort" defaultValue={boLoc.sort}>
            {Object.entries(NHAN_SAP_XEP).map(([gia_tri, khoa]) => (
              <option key={gia_tri} value={gia_tri}>
                {t(locale, khoa)}
              </option>
            ))}
          </select>
        </p>

        <p className="filters__actions">
          <button type="submit">{t(locale, 'products.filter.submit')}</button>
        </p>
      </fieldset>
    </form>
  );
}
