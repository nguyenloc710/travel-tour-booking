import Image from 'next/image';
import type { GalleryImage } from '@travel/api-client';

/**
 * Dải ảnh–chữ xen kẽ, dùng bởi template `ke-chuyen`.
 *
 * Mỗi đoạn của `longDescription` ghép với một tấm trong bộ ảnh, ảnh đổi bên
 * mỗi dải. Ảnh thừa ra không bị bỏ: chúng về khối bộ ảnh ở cuối trang.
 *
 * **Ảnh ở đây KHÔNG minh hoạ đúng đoạn văn cạnh nó**, và điều đó là có ý thức.
 * Không có dữ liệu nào nối một tấm ảnh với một đoạn mô tả — `product_image` chỉ
 * có thứ tự, không có ngữ nghĩa. Nên chữ `alt` giữ nguyên mô tả tấm ảnh, và
 * không có chú thích nào ngụ ý "đây là cảnh của đoạn này". Ngày có dữ liệu thật
 * nối ảnh với ngày lịch trình thì đây là chỗ dùng nó.
 *
 * Số dải bằng số đoạn văn, không bằng số ảnh: đoạn văn là nội dung biên tập
 * viên viết, ảnh chỉ là thứ đi kèm. Hết ảnh thì dải còn lại chỉ có chữ, và nó
 * vẫn đọc được — đó là lý do ảnh nằm trong một nhánh điều kiện chứ không phải
 * một ô lưới cố định.
 */
export function KeChuyen({
  doanVan,
  anh,
}: {
  doanVan: string[];
  anh: GalleryImage[];
}) {
  return (
    <div className="ke-chuyen">
      {doanVan.map((doan, i) => {
        const tam = anh[i];
        return (
          <section
            key={i}
            className={`ke-chuyen__dai ${i % 2 === 1 ? 'ke-chuyen__dai--dao' : ''}`}
          >
            <div className="ke-chuyen__chu">
              <p>{doan}</p>
            </div>
            {tam !== undefined && (
              <div className="ke-chuyen__anh">
                <Image
                  src={tam.url}
                  alt={tam.alt}
                  width={tam.width}
                  height={tam.height}
                  unoptimized
                />
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}
