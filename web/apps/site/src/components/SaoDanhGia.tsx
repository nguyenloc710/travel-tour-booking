import { t, type Locale } from '@travel/i18n';
import { formatNumber } from '@travel/ui';

/**
 * Điểm đánh giá, hiện bằng **sao và chữ cùng lúc**.
 *
 * Dãy sao một mình không đủ: `docs/21` mục 2 quy tắc 2 nói màu và hình không
 * bao giờ là phương tiện duy nhất, và "bốn sao rưỡi" nhìn bằng mắt là bốn ô
 * vàng rưỡi — người dùng trình đọc màn hình, người phân biệt màu kém, và cả
 * người đọc trên màn hình cũ trong phòng sáng đều không đọc ra con số đó.
 *
 * Nên dãy sao mang `aria-hidden`, còn phần trình đọc màn hình đọc là dòng chữ
 * "4,6 af 5 · 87 anmeldelser" — vốn đã có sẵn khoá `products.rating`.
 *
 * Nửa sao dựng bằng một lớp phủ cắt theo phần trăm, không phải bằng ký tự nửa
 * sao: ký tự đó không có trong mọi bộ font, và font là thứ dự án này đã dặn
 * phải thử lại mỗi lần đổi (`docs/21` mục 3.1).
 */
export function SaoDanhGia({
  rating,
  reviewCount,
  locale,
}: {
  rating: number;
  reviewCount: number;
  locale: Locale;
}) {
  // Làm tròn tới một chữ số: 4.9 / 5 ra 98.00000000000001 trong dấu phẩy động
  // của JavaScript, và con số đó đi thẳng vào thuộc tính style của HTML.
  const phanTram = Math.round(Math.max(0, Math.min(100, (rating / 5) * 100)) * 10) / 10;

  return (
    <p className="danh-gia">
      <span className="danh-gia__sao" aria-hidden="true">
        <span className="danh-gia__sao-nen">★★★★★</span>
        <span className="danh-gia__sao-day" style={{ width: `${phanTram}%` }}>
          ★★★★★
        </span>
      </span>

      <span className="danh-gia__chu">
        {t(locale, 'products.rating', {
          rating: formatNumber(rating, locale),
          count: formatNumber(reviewCount, locale),
        })}
      </span>
    </p>
  );
}
