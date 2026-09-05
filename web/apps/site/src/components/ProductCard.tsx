import Link from 'next/link';
import Image from 'next/image';
import { t, type Locale } from '@travel/i18n';
import { formatNumber, PriceFrom } from '@travel/ui';
import type { ProductSummary } from '@travel/api-client';
import { duongDanChiTiet } from '@/lib/routes';
import { SaoDanhGia } from '@/components/SaoDanhGia';

/**
 * Thẻ sản phẩm trong listing.
 *
 * Mọi chữ trên thẻ đều là nội dung **đã dịch** do API trả về — không có chỗ nào
 * ghép chuỗi tiếng Đan vào trang tiếng Việt. Bản ghi chưa dịch không tới được
 * đây: backend đã ẩn nó bằng `INNER JOIN`.
 *
 * **Cả thẻ bấm được, nhưng chỉ có MỘT liên kết.** Bọc thẻ trong `<a>` rồi đặt
 * thêm liên kết bên trong là HTML không hợp lệ; đặt một `<a>` trên ảnh và một
 * trên tiêu đề thì trình đọc màn hình đọc hai lần cùng một đích. Cách ở đây là
 * liên kết ở tiêu đề, và một lớp phủ trong suốt (`::after`) trải rộng ra cả
 * thẻ — vùng bấm bằng cả tấm thẻ, cây accessibility vẫn đúng một mục.
 */
export function ProductCard({ product, locale }: { product: ProductSummary; locale: Locale }) {
  return (
    <article className="the-tour">
      <div className="the-tour__anh">
        <Image
          src={product.heroImage}
          // Chữ alt cũng là nội dung phải dịch, và API trả nó theo locale.
          alt={product.heroImageAlt}
          width={640}
          height={428}
          unoptimized
        />

        {/* Nhãn nằm ĐÈ lên ảnh, nên nền của chúng phải đục — chữ trắng trên một
            tấm ảnh sáng là chỗ tương phản hỏng mà không ai đo. */}
        <p className="the-tour__nhan">
          <span className="badge">{t(locale, `productType.${product.productType}`)}</span>
          {product.isNew && <span className="badge badge--new">{t(locale, 'products.isNew')}</span>}
        </p>
      </div>

      <div className="the-tour__than">
        {/* Dòng "ở đâu · bao lâu" đứng TRÊN tiêu đề, không đứng dưới: nó là
            thứ khách lọc bằng mắt khi lướt lưới, còn tiêu đề là thứ họ đọc sau
            khi đã thấy dòng ấy đúng ý. */}
        <p className="the-tour__noi">
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

        <h3 className="the-tour__ten">
          <Link href={duongDanChiTiet(locale, product.slug)}>{product.title}</Link>
        </h3>

        <p className="the-tour__tom-tat">{product.shortDescription}</p>

        <div className="the-tour__chan">
          {/* Sản phẩm chưa có đánh giá nào thì nói thẳng là chưa có, chứ không
              để trống: một khoảng trắng ở chỗ người ta chờ số sao đọc như là
              trang tải hỏng. */}
          {product.rating !== undefined ? (
            <SaoDanhGia rating={product.rating} reviewCount={product.reviewCount} locale={locale} />
          ) : (
            <p className="danh-gia danh-gia--chua">{t(locale, 'products.noReviews')}</p>
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
      </div>
    </article>
  );
}
