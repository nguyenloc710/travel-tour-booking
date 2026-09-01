import Link from 'next/link';
import { t, type Locale } from '@travel/i18n';
import { formatNumber } from '@travel/ui';
import type { ProductPage } from '@travel/api-client';
import { chuoiTruyVan, type BoLoc } from '@/lib/filters';
import { duongDanListing } from '@/lib/routes';

/**
 * Phân trang bằng **chữ đọc được**, không dùng chấm tròn.
 *
 * "Đang xem 1–12 trên 47" nói đúng ba điều mà một dãy chấm không nói: đang ở
 * đâu, còn bao nhiêu, và tổng là bao nhiêu. Khách lớn tuổi Bắc Âu là nhóm mà
 * dãy chấm phục vụ kém nhất (web/CLAUDE.md mục 5.4 và mục 6).
 *
 * Hai nút là `<a>` thật với đường dẫn thật, nên mở tab mới được và Back hoạt
 * động — mọi bộ lọc đang bật đều được giữ nguyên trong đường dẫn đó.
 */
export function Pagination({
  locale,
  trang,
  boLoc,
}: {
  locale: Locale;
  trang: ProductPage;
  boLoc: BoLoc;
}) {
  if (trang.totalPages <= 1) return null;

  const tu = trang.page * trang.size + 1;
  const den = Math.min(tu + trang.items.length - 1, trang.totalItems);
  const coTruoc = trang.page > 0;
  const coSau = trang.page + 1 < trang.totalPages;

  return (
    <nav className="pagination" aria-label={t(locale, 'products.heading')}>
      <p className="pagination__status" aria-live="polite">
        {t(locale, 'pagination.status', {
          from: formatNumber(tu, locale),
          to: formatNumber(den, locale),
          total: formatNumber(trang.totalItems, locale),
        })}
      </p>

      <p className="pagination__links">
        {coTruoc && (
          <Link
            rel="prev"
            href={duongDanListing(locale, chuoiTruyVan(boLoc, { page: trang.page - 1 }))}
          >
            {t(locale, 'pagination.prev')}
          </Link>
        )}
        {coSau && (
          <Link
            rel="next"
            href={duongDanListing(locale, chuoiTruyVan(boLoc, { page: trang.page + 1 }))}
          >
            {t(locale, 'pagination.next')}
          </Link>
        )}
      </p>
    </nav>
  );
}
