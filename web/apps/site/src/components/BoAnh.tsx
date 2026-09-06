import Image from 'next/image';
import { t, type Locale } from '@travel/i18n';
import type { GalleryImage } from '@travel/api-client';

/**
 * Bộ ảnh của sản phẩm — khối 8 của `docs/05` mục 5.1.
 *
 * <p>Khối này nằm trong tài liệu từ đầu nhưng không dựng được, vì chưa biết ảnh
 * lưu ở đâu (**Q-6**). Q-6 chốt ngày 06/09 bằng ADR-011, và đây là khối đầu tiên
 * ăn theo quyết định đó.
 *
 * **Rỗng thì không hiện gì, kể cả tiêu đề.** Sản phẩm chưa có ảnh là trạng thái
 * hợp lệ, và một tiêu đề "Bộ ảnh" trên một khoảng trắng nói với khách rằng trang
 * đang hỏng.
 *
 * `kieu` để các template gọi cùng một khối với bố cục khác nhau — lưới đều cho
 * bản cổ điển, mosaic cho bản ảnh lớn. Chia bố cục bằng CSS chứ không bằng hai
 * component: dữ liệu, thứ tự, chữ `alt` và hành vi rỗng phải giống hệt nhau ở
 * mọi template, và tách component là mở đường cho chúng lệch nhau.
 */
export function BoAnh({
  anh,
  locale,
  kieu = 'luoi',
}: {
  anh: GalleryImage[] | undefined;
  locale: Locale;
  kieu?: 'luoi' | 'mosaic';
}) {
  if (anh === undefined || anh.length === 0) {
    return null;
  }

  return (
    <section className="bo-anh">
      <h2>{t(locale, 'detail.gallery')}</h2>
      <ul className={`bo-anh__luoi bo-anh__luoi--${kieu}`}>
        {anh.map((a) => (
          <li key={a.url}>
            <Image
              src={a.url}
              // `alt` mô tả TẤM ẢNH, không mô tả sản phẩm — `24` mục 6. API trả
              // nó theo locale, và ảnh không có `alt` ở locale đang đọc thì
              // backend đã loại khỏi danh sách này rồi.
              alt={a.alt}
              width={a.width}
              height={a.height}
              unoptimized
            />
          </li>
        ))}
      </ul>
    </section>
  );
}
