import Link from 'next/link';
import { t, type Locale } from '@travel/i18n';
import { formatNumber } from '@travel/ui';
import type { Destination } from '@travel/api-client';
import { duongDanDiemDen } from '@/lib/routes';

/**
 * Danh sách điểm đến, gom theo miền (R5).
 *
 * Thứ tự do **API** quyết, không do frontend sắp lại: backend đã sắp theo miền
 * rồi tới thứ tự trong miền, để danh sách đọc được như một hành trình từ Bắc
 * vào Nam chứ không phải một mớ theo bảng chữ cái. Gọi `.sort()` ở đây là phá
 * đúng tính chất đó.
 *
 * `productCount` **đếm từ dữ liệu** trong phạm vi `(market, locale)` đang xem —
 * hai locale ra hai con số khác nhau, vì điểm đến chưa dịch bị ẩn hoàn toàn và
 * sản phẩm chưa dịch cũng vậy.
 */
export function DestinationList({
  locale,
  diemDen,
}: {
  locale: Locale;
  diemDen: Destination[];
}) {
  const theoMien = gomTheoMien(diemDen);

  return (
    <>
      {theoMien.map(([mien, ds]) => (
        <section key={mien.slug} className="dd-mien">
          <h2>{mien.name}</h2>
          <ul className="dd-luoi">
            {ds.map((d) => (
              <li key={d.slug} className="dd-the">
                <Link href={duongDanDiemDen(locale, d.slug)}>
                  <h3>{d.name}</h3>
                </Link>
                {d.summary && <p className="dd-tom-tat">{d.summary}</p>}
                <p className="dd-so">
                  {d.productCount === 0
                    ? t(locale, 'destinations.productCount.none')
                    : t(
                        locale,
                        d.productCount === 1
                          ? 'destinations.productCount.one'
                          : 'destinations.productCount.many',
                        { count: formatNumber(d.productCount, locale) },
                      )}
                </p>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </>
  );
}

/**
 * Gom theo miền, **giữ nguyên thứ tự API trả về**.
 *
 * Dùng `Map` chứ không object: `Map` giữ thứ tự chèn cho mọi loại khoá, còn
 * object thì JavaScript sắp lại khoá dạng số theo giá trị — và slug miền hoàn
 * toàn có thể là chuỗi số.
 */
function gomTheoMien(diemDen: Destination[]): [Destination['region'], Destination[]][] {
  const nhom = new Map<string, [Destination['region'], Destination[]]>();
  for (const d of diemDen) {
    const co = nhom.get(d.region.slug);
    if (co) {
      co[1].push(d);
    } else {
      nhom.set(d.region.slug, [d.region, [d]]);
    }
  }
  return [...nhom.values()];
}
