import Image from 'next/image';
import Link from 'next/link';
import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatNumber, PriceFrom } from '@travel/ui';
import type { ProductDetail } from '@travel/api-client';
import { laKhongTimThay, productsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import { duongDanListing, laProductsSegment } from '@/lib/routes';
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
  if (!isLocale(locale) || !laProductsSegment(locale, section)) return {};

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
