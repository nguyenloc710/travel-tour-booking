import { Suspense } from 'react';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import type { Metadata } from 'next';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatNumber } from '@travel/ui';
import type { Destination, ProductPage, ProductSort, ProductType } from '@travel/api-client';
import { destinationsApi, productsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import {
  duongDanDiemDen,
  duongDanListing,
  duongDanTimTour,
  laDestinationsSegment,
  laProductsSegment,
  laTourFinderSegment,
} from '@/lib/routes';
import { ProductCard } from '@/components/ProductCard';
import { ProductFilters } from '@/components/ProductFilters';
import { Pagination } from '@/components/Pagination';
import { DestinationList } from '@/components/DestinationList';
import { chuoiTruyVan, docBoLoc, type BoLoc } from '@/lib/filters';

type Params = Promise<{ locale: string; section: string }>;
type Search = Promise<Record<string, string | string[] | undefined>>;

export async function generateMetadata({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: Search;
}): Promise<Metadata> {
  const { locale, section } = await params;
  if (!isLocale(locale)) return {};

  if (laDestinationsSegment(locale, section)) {
    return {
      title: `${t(locale, 'destinations.heading')} · ${t(locale, 'site.name')}`,
      description: t(locale, 'destinations.intro'),
      alternates: { canonical: duongDanDiemDen(locale) },
    };
  }

  if (laTourFinderSegment(locale, section)) {
    return {
      title: `${t(locale, 'tourFinder.heading')} · ${t(locale, 'site.name')}`,
      description: t(locale, 'tourFinder.intro'),
      alternates: { canonical: duongDanTimTour(locale) },
    };
  }

  const coBoLoc = chuoiTruyVan(docBoLoc(await searchParams)).toString() !== '';

  return {
    title: `${t(locale, 'products.heading')} · ${t(locale, 'site.name')}`,
    // Canonical luôn trỏ về đường dẫn KHÔNG có tham số lọc, và trang có lọc thì
    // noindex/follow — docs/20 mục 6. Mỗi tổ hợp bộ lọc là một URL; để công cụ
    // tìm kiếm thu thập hết thì ngân sách thu thập tiêu vào các biến thể gần
    // giống nhau thay vì tiêu vào trang sản phẩm thật.
    alternates: { canonical: duongDanListing(locale) },
    ...(coBoLoc ? { robots: { index: false, follow: true } } : {}),
  };
}

/**
 * Danh sách sản phẩm.
 *
 * Đoạn đường dẫn được kiểm theo locale: `/da/tour` trả 404 vì `tour` là đoạn
 * tiếng Việt. Không nhận cả hai để "cho tiện" — hai URL cùng nội dung là nội
 * dung trùng lặp với công cụ tìm kiếm.
 */
export default async function ProductListPage({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: Search;
}) {
  const { locale, section } = await params;
  if (!isLocale(locale)) notFound();

  // Một đoạn động, ba trang. Đoạn nhận được đối chiếu với bảng ánh xạ CỦA LOCALE
  // ĐANG XEM — `/da/tour` trả 404 vì `tour` là đoạn tiếng Việt (docs/20 mục 2.1).
  if (laDestinationsSegment(locale, section)) {
    return <DestinationsPage locale={locale} />;
  }
  if (laTourFinderSegment(locale, section)) {
    return <TourFinderPage locale={locale} thamSo={await searchParams} />;
  }
  if (!laProductsSegment(locale, section)) notFound();

  // Trạng thái bộ lọc nằm TRONG URL, không trong useState: F5 giữ nguyên bộ
  // lọc, và dán link ra đúng kết quả đó (web/CLAUDE.md mục 5.2).
  const boLoc = docBoLoc(await searchParams);

  return (
    <section className="listing">
      <h1>{t(locale, 'products.heading')}</h1>

      <ProductFilters locale={locale} boLoc={boLoc} />

      <Suspense key={JSON.stringify(boLoc)} fallback={<ProductSkeleton />}>
        <ProductList locale={locale} boLoc={boLoc} />
      </Suspense>
    </section>
  );
}

async function ProductList({ locale, boLoc }: { locale: Locale; boLoc: BoLoc }) {
  const { market } = await resolveMarket(locale);

  let trang: ProductPage;
  try {
    trang = await productsApi().listProducts({
      ...requestScope(market, locale),
      region: boLoc.region,
      productType: boLoc.productType as ProductType | undefined,
      q: boLoc.q,
      sort: boLoc.sort as ProductSort,
      page: boLoc.page,
      size: boLoc.size,
    });
  } catch (loi) {
    // KHÔNG hiện mã lỗi ra khách; ghi log rồi hiện câu chung.
    console.error('[api] listProducts thất bại', loi);
    return (
      <div className="state state--error" role="alert">
        <p className="state__title">{t(locale, 'state.error.title')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  if (trang.items.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'state.empty.title')}</p>
        <p>{t(locale, 'state.empty.help')}</p>
      </div>
    );
  }

  return (
    <>
      {/* Số đếm TỪ DỮ LIỆU, trong phạm vi (market, locale) đang xem. Cùng một
          thị trường, hai locale ra hai con số khác nhau vì bản ghi chưa dịch
          bị ẩn hoàn toàn — đó là lý do con số này không được hardcode. */}
      <p className="listing__total" aria-live="polite">
        {/* Số ít và số nhiều là hai khoá riêng: tiếng Đan viết "1 rejse" chứ
            không "1 rejser". Tiếng Việt không phân biệt nên hai khoá trùng nội
            dung — vẫn giữ đủ hai, để bộ kiểm độ phủ so được từng khoá một. */}
        {t(locale, trang.totalItems === 1 ? 'products.total.one' : 'products.total.many', {
          count: formatNumber(trang.totalItems, locale),
        })}
      </p>

      <ul className="product-grid">
        {trang.items.map((sp) => (
          <li key={sp.slug}>
            <ProductCard product={sp} locale={locale} />
          </li>
        ))}
      </ul>

      <Pagination locale={locale} trang={trang} boLoc={boLoc} />
    </>
  );
}

/** Skeleton, không phải vòng xoay — web/CLAUDE.md mục 5.3. */
function ProductSkeleton() {
  return (
    <ul className="product-grid product-grid--skeleton" aria-hidden="true">
      {[0, 1, 2, 3, 4, 5].map((i) => (
        <li key={i} className="product-card product-card--skeleton">
          <span className="skeleton-bar skeleton-bar--image" />
          <span className="skeleton-bar" />
          <span className="skeleton-bar skeleton-bar--short" />
        </li>
      ))}
    </ul>
  );
}

/* ------------------------------------------------------------ R5 điểm đến */

/**
 * Danh sách điểm đến (R5).
 *
 * Không có bộ lọc: đây là trang **duyệt**, dành cho khách chưa biết mình muốn
 * đi đâu. Khách đã biết thì vào thẳng danh sách tour, và trang tìm tour (R2) là
 * đường ở giữa hai nhóm đó.
 */
async function DestinationsPage({ locale }: { locale: Locale }) {
  return (
    <section className="listing">
      <h1>{t(locale, 'destinations.heading')}</h1>
      <p className="listing__intro">{t(locale, 'destinations.intro')}</p>

      <Suspense fallback={<ProductSkeleton />}>
        <DestinationsData locale={locale} />
      </Suspense>
    </section>
  );
}

async function DestinationsData({ locale }: { locale: Locale }) {
  const { market } = await resolveMarket(locale);

  let diemDen: Destination[];
  try {
    diemDen = await destinationsApi().listDestinations(requestScope(market, locale));
  } catch (loi) {
    console.error('[api] listDestinations thất bại', loi);
    return <TrangThaiLoi locale={locale} />;
  }

  if (diemDen.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'destinations.empty')}</p>
        <p>{t(locale, 'state.empty.help')}</p>
      </div>
    );
  }

  return <DestinationList locale={locale} diemDen={diemDen} />;
}

/* ------------------------------------------------------------ R2 tìm tour */

/**
 * Tìm tour — bộ lọc phân cấp miền → điểm đến (R2).
 *
 * **Khác trang danh sách sản phẩm, không phải trùng.** Trang này dẫn khách chưa
 * biết mình muốn gì đi qua hai bước; trang danh sách là bảng phẳng cho khách đã
 * biết. Hai trang gọi cùng endpoint nhưng khác nhau ở cách dẫn dắt (docs/20
 * mục 2).
 *
 * Bước đang chọn nằm trong **URL**, nên F5 và dán link đều giữ nguyên — và nó
 * chạy được cả khi trình duyệt tắt JavaScript, vì mỗi lựa chọn là một liên kết.
 */
async function TourFinderPage({
  locale,
  thamSo,
}: {
  locale: Locale;
  thamSo: Record<string, string | string[] | undefined>;
}) {
  const mien = typeof thamSo.region === 'string' ? thamSo.region : undefined;

  return (
    <section className="listing">
      <h1>{t(locale, 'tourFinder.heading')}</h1>
      <p className="listing__intro">{t(locale, 'tourFinder.intro')}</p>

      <Suspense key={mien ?? ''} fallback={<ProductSkeleton />}>
        <TourFinderData locale={locale} mien={mien} />
      </Suspense>

      <p className="listing__intro">
        <Link href={duongDanListing(locale)}>{t(locale, 'tourFinder.toList')}</Link>
      </p>
    </section>
  );
}

async function TourFinderData({ locale, mien }: { locale: Locale; mien: string | undefined }) {
  const { market } = await resolveMarket(locale);

  let diemDen: Destination[];
  try {
    diemDen = await destinationsApi().listDestinations({
      ...requestScope(market, locale),
      region: mien,
    });
  } catch (loi) {
    console.error('[api] listDestinations thất bại', loi);
    return <TrangThaiLoi locale={locale} />;
  }

  // Danh sách miền suy TỪ CHÍNH dữ liệu điểm đến, không gọi thêm /regions: miền
  // không có điểm đến nào đã dịch thì cũng không có gì để chọn ở bước hai, nên
  // hiện nó ra chỉ dẫn tới một danh sách rỗng.
  const mienCo = new Map<string, string>();
  for (const d of diemDen) {
    mienCo.set(d.region.slug, d.region.name);
  }

  return (
    <>
      <fieldset className="tf-buoc">
        <legend>{t(locale, 'tourFinder.step.region')}</legend>
        <Link
          href={duongDanTimTour(locale)}
          aria-current={mien ? undefined : 'true'}
          className="tf-chip"
        >
          {t(locale, 'tourFinder.allRegions')}
        </Link>
        {[...mienCo].map(([slug, ten]) => (
          <Link
            key={slug}
            href={duongDanTimTour(locale, new URLSearchParams({ region: slug }))}
            aria-current={mien === slug ? 'true' : undefined}
            className="tf-chip"
          >
            {ten}
          </Link>
        ))}
      </fieldset>

      <fieldset className="tf-buoc">
        <legend>{t(locale, 'tourFinder.step.destination')}</legend>
        {diemDen.length === 0 ? (
          <p className="state__title">{t(locale, 'destinations.empty')}</p>
        ) : (
          <DestinationList locale={locale} diemDen={diemDen} />
        )}
      </fieldset>
    </>
  );
}

function TrangThaiLoi({ locale }: { locale: Locale }) {
  return (
    <div className="state state--error" role="alert">
      <p className="state__title">{t(locale, 'state.error.title')}</p>
      <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
    </div>
  );
}
