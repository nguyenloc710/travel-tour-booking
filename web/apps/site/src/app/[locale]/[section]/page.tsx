import { Suspense } from 'react';
import { notFound } from 'next/navigation';
import type { Metadata } from 'next';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatNumber } from '@travel/ui';
import type { ProductPage, ProductSort, ProductType } from '@travel/api-client';
import { productsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import { duongDanListing, laProductsSegment } from '@/lib/routes';
import { ProductCard } from '@/components/ProductCard';
import { ProductFilters } from '@/components/ProductFilters';
import { Pagination } from '@/components/Pagination';
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
  const { locale } = await params;
  if (!isLocale(locale)) return {};

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
