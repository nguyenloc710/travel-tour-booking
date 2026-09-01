import { Suspense } from 'react';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatNumber } from '@travel/ui';
import { regionsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import { duongDanListing } from '@/lib/routes';

export default async function HomePage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  if (!isLocale(locale)) notFound();

  return (
    <section>
      <p className="home__cta">
        <Link href={duongDanListing(locale)}>{t(locale, 'products.heading')}</Link>
      </p>

      <h1>{t(locale, 'region.heading')}</h1>
      {/* Trạng thái ĐANG TẢI là skeleton, không phải vòng xoay — web/CLAUDE.md mục 5.3 */}
      <Suspense fallback={<RegionSkeleton />}>
        <RegionList locale={locale} />
      </Suspense>
    </section>
  );
}

async function RegionList({ locale }: { locale: Locale }) {
  const { market } = await resolveMarket(locale);

  let mien;
  try {
    mien = await regionsApi().listRegions(requestScope(market, locale));
  } catch (loi) {
    // KHÔNG hiện mã lỗi ra khách. Ghi log để bổ sung bản dịch, hiện câu chung.
    console.error('[api] listRegions thất bại', loi);
    return (
      <div className="state state--error" role="alert">
        <p className="state__title">{t(locale, 'state.error.title')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  // Trạng thái RỖNG kèm hướng dẫn hành động, không phải một dòng "không có gì".
  if (mien.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'state.empty.title')}</p>
        <p>{t(locale, 'state.empty.help')}</p>
      </div>
    );
  }

  return (
    <ul className="region-list">
      {mien.map((m) => (
        <li key={m.slug} className="region-list__item">
          <span className="region-list__name">{m.name}</span>{' '}
          {/* Số đếm TỪ DỮ LIỆU, trong phạm vi (market, locale) đang xem.
              Con số này khác nhau giữa hai locale vì bản ghi chưa dịch bị ẩn. */}
          <span className="region-list__count">
            {t(locale, 'region.productCount', {
              count: formatNumber(m.productCount, locale),
            })}
          </span>
        </li>
      ))}
    </ul>
  );
}

function RegionSkeleton() {
  return (
    <ul className="region-list region-list--skeleton" aria-hidden="true">
      {[0, 1, 2].map((i) => (
        <li key={i} className="region-list__item">
          <span className="skeleton-bar" />
        </li>
      ))}
    </ul>
  );
}
