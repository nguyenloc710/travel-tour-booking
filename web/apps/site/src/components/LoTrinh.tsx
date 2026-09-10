import Image from 'next/image';
import { t, type Locale, type Market } from '@travel/i18n';
import { formatNumber } from '@travel/ui';
import type { ProductStop } from '@travel/api-client';
import { productsApi, requestScope } from '@/lib/api';

/**
 * Khối "bản đồ lộ trình" của tab Tổng quan — `docs/05` mục 6.1 vẽ nó là
 * `[bản đồ lộ trình]  Nơi đến | Số đêm | Điểm nhấn`.
 *
 * **Danh sách chặng dựng từ lịch trình**, không phải một danh sách nhập riêng:
 * mỗi điểm đến một dòng, số đêm bằng số ngày ngủ ở đó. Máy chủ làm việc gom nhóm
 * đó (`GET /{market}/products/{slug}/stops`), nên hai chỗ không thể lệch nhau.
 *
 * **Không phải bản đồ tương tác.** Bản đồ ở đây là ảnh tĩnh mà biên tập viên
 * chuẩn bị; bản đồ bấm được nằm trong danh sách cố tình chưa làm ở v1
 * (`docs/01` mục 3.2).
 *
 * Ảnh và video là **của điểm đến**, không của tour: cùng một Sa Pa xuất hiện
 * trong nhiều chuyến, và chụp lại cho từng chuyến là nhân bản cả tệp lẫn chứng
 * từ giấy phép.
 */
export async function LoTrinh({
  slug,
  locale,
  market,
  mapImage,
  mapAlt,
}: {
  slug: string;
  locale: Locale;
  market: Market;
  mapImage?: string;
  mapAlt: string;
}) {
  let chang: ProductStop[];
  try {
    chang = await productsApi().getProductStops({ ...requestScope(market, locale), slug });
  } catch {
    // Loại sản phẩm không có lộ trình trả 404 — khối này biến mất, không phải
    // hiện ra rỗng. Cùng cách xử lý với mọi tab không có mặt theo loại.
    return null;
  }

  if (chang.length === 0 && mapImage === undefined) {
    return null;
  }

  return (
    <section className="lo-trinh">
      <h2>{t(locale, 'detail.stops.title')}</h2>

      {mapImage !== undefined && (
        <Image
          className="detail__map"
          src={mapImage}
          // Bản đồ cũng là ảnh có chữ: font của nó phải dựng được cả `æ ø å` lẫn
          // dấu tiếng Việt — bản demo đã dính bẫy này một lần.
          alt={mapAlt}
          width={1200}
          height={800}
          unoptimized
        />
      )}

      {chang.map((c) => (
        <article key={c.destination.slug} className="the">
          <h3>{c.destination.name}</h3>
          <p className="phu">
            {t(locale, c.nights === 1 ? 'detail.hotels.night' : 'detail.hotels.nights', {
              count: formatNumber(c.nights, locale),
            })}
            {' · '}
            {t(locale, 'detail.stops.days', { days: khoangNgay(c.dayNumbers, locale) })}
          </p>

          {c.media.map((m) =>
            m.kind === 'VIDEO' ? (
              // `preload="metadata"` chứ không `auto`: kho ảnh là MinIO tự dựng,
              // không có CDN, nên tải sẵn cả video cho một khách chưa bấm xem là
              // băng thông trả cho thứ không ai dùng (ADR-011 mục 5).
              <video
                key={m.url}
                className="lo-trinh__video"
                controls
                preload="metadata"
                poster={m.posterUrl}
                aria-label={m.alt}
                width={m.width}
                height={m.height}
              >
                <source src={m.url} type={m.contentType} />
                {t(locale, 'detail.stops.videoFallback')}
              </video>
            ) : (
              <Image
                key={m.url}
                src={m.url}
                alt={m.alt}
                width={m.width}
                height={m.height}
                unoptimized
              />
            ),
          )}
        </article>
      ))}
    </section>
  );
}

/**
 * `[1, 2, 3]` thành `1–3`, `[4]` thành `4`.
 *
 * Ngày trong một chặng gần như luôn liền nhau, nhưng không có gì bắt buộc thế:
 * một tour có thể quay lại Hà Nội ở ngày cuối. Chuỗi rời thì liệt kê, không rút
 * thành khoảng — `1, 2, 14` nói đúng, còn `1–14` thì nói sai.
 */
function khoangNgay(ngay: number[], locale: Locale): string {
  const so = ngay.map((n) => formatNumber(n, locale));
  const lienNhau = ngay.every((n, i) => i === 0 || n === (ngay[i - 1] ?? 0) + 1);

  if (so.length === 0) {
    return '';
  }
  if (so.length === 1 || !lienNhau) {
    return so.join(', ');
  }
  return `${so[0]}–${so[so.length - 1]}`;
}
