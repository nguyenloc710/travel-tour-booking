import Link from 'next/link';
import Image from 'next/image';
import { t, type Locale } from '@travel/i18n';
import { formatNumber, PriceFrom } from '@travel/ui';
import type { ProductSummary } from '@travel/api-client';
import { duongDanChiTiet } from '@/lib/routes';

/**
 * Thẻ sản phẩm trong listing.
 *
 * Mọi chữ trên thẻ đều là nội dung **đã dịch** do API trả về — không có chỗ nào
 * ghép chuỗi tiếng Đan vào trang tiếng Việt. Bản ghi chưa dịch không tới được
 * đây: backend đã ẩn nó bằng `INNER JOIN`.
 */
export function ProductCard({ product, locale }: { product: ProductSummary; locale: Locale }) {
  return (
    <article className="product-card">
      <Image
        className="product-card__image"
        src={product.heroImage}
        // Chữ alt cũng là nội dung phải dịch, và API trả nó theo locale.
        alt={product.heroImageAlt}
        width={480}
        height={320}
        unoptimized
      />

      <div className="product-card__body">
        <p className="product-card__meta">
          <span className="badge">{t(locale, `productType.${product.productType}`)}</span>
          {product.isNew && <span className="badge badge--new">{t(locale, 'products.isNew')}</span>}
        </p>

        <h2 className="product-card__title">
          <Link href={duongDanChiTiet(locale, product.slug)}>{product.title}</Link>
        </h2>

        <p className="product-card__where">
          {product.destination.name} · {product.region.name}
          {product.durationDays !== undefined && (
            <>
              {' · '}
              {t(locale, 'products.duration', {
                days: formatNumber(product.durationDays, locale),
              })}
            </>
          )}
        </p>

        <p className="product-card__summary">{product.shortDescription}</p>

        {product.rating !== undefined && (
          <p className="product-card__rating">
            {t(locale, 'products.rating', {
              rating: formatNumber(product.rating, locale),
              count: formatNumber(product.reviewCount, locale),
            })}
          </p>
        )}

        {/* Giá đi qua PriceFrom nên luôn kèm chữ "từ" và disclaimer — yêu cầu
            pháp lý. Chưa có bảng giá ở thị trường này thì mời khách gọi điện,
            KHÔNG hiện số 0. */}
        {product.priceFrom ? (
          <PriceFrom price={product.priceFrom} locale={locale} />
        ) : (
          <p className="price-from price-from--contact">{t(locale, 'products.contactForPrice')}</p>
        )}
      </div>
    </article>
  );
}
