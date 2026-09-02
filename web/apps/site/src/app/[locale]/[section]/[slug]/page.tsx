import { Suspense } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatNumber, PriceFrom } from '@travel/ui';
import type { Destination, ProductDetail } from '@travel/api-client';
import { destinationsApi, laKhongTimThay, productsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import {
  duongDanDiemDen,
  duongDanListing,
  laDestinationsSegment,
  laProductsSegment,
} from '@/lib/routes';
import { ProductCard } from '@/components/ProductCard';
import { ProductFacts } from '@/components/ProductFacts';

type Params = Promise<{ locale: string; section: string; slug: string }>;

/**
 * Lấy sản phẩm, hoặc 404.
 *
 * **404 ở đây gộp ba tình huống có chủ ý**: không tồn tại, chưa dịch sang
 * locale này, chưa bán ở thị trường này. Backend đã gộp sẵn (docs/13 mục 5.1),
 * và frontend **không** được đi tìm bản `da` để lấp chỗ trống — đó chính là
 * chính sách không-fallback của docs/02 mục 4.
 */
/**
 * Lấy điểm đến, hoặc 404 — cùng chính sách với sản phẩm.
 *
 * `404` gộp ba tình huống: không tồn tại, chưa dịch cho locale này, hoặc miền
 * chứa nó đã bị xoá mềm. Backend gộp sẵn, và frontend **không** đi tìm bản `da`
 * để lấp chỗ trống.
 */
async function layDiemDen(locale: Locale, slug: string): Promise<Destination> {
  const { market } = await resolveMarket(locale);
  try {
    return await destinationsApi().getDestination({ ...requestScope(market, locale), slug });
  } catch (loi) {
    if (laKhongTimThay(loi)) notFound();
    throw loi;
  }
}

async function laySanPham(locale: Locale, slug: string): Promise<ProductDetail> {
  const { market } = await resolveMarket(locale);
  try {
    return await productsApi().getProduct({ ...requestScope(market, locale), slug });
  } catch (loi) {
    if (laKhongTimThay(loi)) notFound();
    // Lỗi mạng không phải "không tồn tại" — ném tiếp để error boundary lo,
    // đừng biến một sự cố backend thành trang 404 và mất khách thật.
    throw loi;
  }
}

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { locale, section, slug } = await params;
  if (!isLocale(locale)) return {};

  if (laDestinationsSegment(locale, section)) {
    const diemDen = await layDiemDen(locale, slug);
    return {
      title: `${diemDen.name} · ${t(locale, 'site.name')}`,
      description: diemDen.summary,
      alternates: { canonical: duongDanDiemDen(locale, slug) },
    };
  }

  if (!laProductsSegment(locale, section)) return {};

  const sanPham = await laySanPham(locale, slug);
  return {
    title: `${sanPham.title} · ${t(locale, 'site.name')}`,
    description: sanPham.shortDescription,
    // KHÔNG khai báo hreflang cho locale kia: slug phụ thuộc locale và trang
    // này không biết bản dịch bên kia có tồn tại hay không. Khai bừa một URL
    // trả 404 còn tệ hơn không khai (docs/02 mục 5.2).
  };
}

export default async function ProductDetailPage({ params }: { params: Params }) {
  const { locale, section, slug } = await params;
  if (!isLocale(locale)) notFound();

  if (laDestinationsSegment(locale, section)) {
    return <DestinationDetailPage locale={locale} slug={slug} />;
  }
  if (!laProductsSegment(locale, section)) notFound();

  const sanPham = await laySanPham(locale, slug);

  return (
    <article className="detail">
      <p className="detail__breadcrumb">
        <Link href={duongDanListing(locale)}>{t(locale, 'detail.backToList')}</Link>
      </p>

      <header className="detail__header">
        <p className="detail__meta">
          <span className="badge">{t(locale, `productType.${sanPham.productType}`)}</span>
          <span>
            {sanPham.destination.name} · {sanPham.region.name}
          </span>
          {sanPham.durationDays !== undefined && (
            <span>
              {t(locale, 'products.duration', {
                days: formatNumber(sanPham.durationDays, locale),
              })}
            </span>
          )}
        </p>

        <h1>{sanPham.title}</h1>
        <p className="detail__summary">{sanPham.shortDescription}</p>

        {sanPham.priceFrom ? (
          <PriceFrom price={sanPham.priceFrom} locale={locale} />
        ) : (
          <p className="price-from price-from--contact">{t(locale, 'products.contactForPrice')}</p>
        )}

        {sanPham.productType === 'PRIVATE_TOUR' && (
          <p className="detail__cta">
            <strong>{t(locale, 'detail.private.cta')}</strong>
          </p>
        )}
      </header>

      <Image
        className="detail__image"
        src={sanPham.heroImage}
        alt={sanPham.heroImageAlt}
        width={1200}
        height={640}
        priority
        unoptimized
      />

      <div className="detail__body">
        {/* Từng đoạn văn một, không phải một khối HTML: nội dung do biên tập
            viên nhập, và HTML tự do từ CSDL là lỗ chèn mã chờ sẵn. */}
        {sanPham.longDescription.map((doan, i) => (
          <p key={i}>{doan}</p>
        ))}
      </div>

      <section className="why">
        <h2>{t(locale, 'detail.whyChooseThis')}</h2>
        <ul>
          {sanPham.whyChooseThis.map((ly_do) => (
            <li key={ly_do}>{ly_do}</li>
          ))}
        </ul>
      </section>

      <ProductFacts product={sanPham} locale={locale} />

      {sanPham.mapImage !== undefined && (
        <Image
          className="detail__map"
          src={sanPham.mapImage}
          // Bản đồ cũng là ảnh có chữ: font của nó phải dựng được cả `æ ø å`
          // lẫn dấu tiếng Việt — bản demo đã dính bẫy này một lần.
          alt={sanPham.heroImageAlt}
          width={1200}
          height={800}
          unoptimized
        />
      )}
    </article>
  );
}

/* ------------------------------------------------------------ R6 điểm đến */

/**
 * Trang một điểm đến (R6): giới thiệu, rồi danh sách tour tới đó.
 *
 * Danh sách tour lọc bằng tham số `destination` của endpoint sản phẩm — cùng
 * một endpoint mà trang danh sách dùng, chỉ khác bộ lọc. Không có endpoint
 * riêng "tour theo điểm đến", và cũng không cần: thêm một đường đọc thứ hai cho
 * cùng dữ liệu là thêm một chỗ để quên điều kiện `NOT soft_delete`.
 */
async function DestinationDetailPage({ locale, slug }: { locale: Locale; slug: string }) {
  const diemDen = await layDiemDen(locale, slug);

  return (
    <article className="detail">
      <p className="detail__breadcrumb">
        <Link href={duongDanDiemDen(locale)}>{t(locale, 'destinations.heading')}</Link>
      </p>

      <h1>{diemDen.name}</h1>
      <p className="detail__meta">{diemDen.region.name}</p>
      {diemDen.summary && <p>{diemDen.summary}</p>}

      <h2>{t(locale, 'destinations.viewProducts')}</h2>
      <Suspense fallback={<div className="skeleton-bar" />}>
        <TourTaiDiemDen locale={locale} slug={slug} />
      </Suspense>
    </article>
  );
}

async function TourTaiDiemDen({ locale, slug }: { locale: Locale; slug: string }) {
  const { market } = await resolveMarket(locale);

  let trang;
  try {
    trang = await productsApi().listProducts({
      ...requestScope(market, locale),
      destination: slug,
      size: 12,
    });
  } catch (loi) {
    console.error('[api] listProducts theo điểm đến thất bại', loi);
    return (
      <div className="state state--error" role="alert">
        <p className="state__title">{t(locale, 'state.error.title')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  // Rỗng KÈM hướng dẫn hành động, không phải một dòng "không có gì" cụt lủn
  // (web/CLAUDE.md mục 5.3): điểm đến chưa có tour vẫn là khách quan tâm tới nơi
  // đó, và đó là lúc đáng mời họ gọi điện nhất.
  if (trang.items.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'destinations.noProducts')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  return (
    <ul className="product-grid">
      {trang.items.map((sp) => (
        <ProductCard key={sp.slug} locale={locale} product={sp} />
      ))}
    </ul>
  );
}
