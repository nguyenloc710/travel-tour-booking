/**
 * Danh mục template của trang chi tiết — `docs/05`.
 *
 * **Danh mục nằm ở đây chứ không nằm trong CSDL.** `product.layout` là chuỗi tự
 * do (`12` mục 4.1): thêm một template là thêm một component và một dòng trong
 * bảng dưới đây, không phải một migration. Để CSDL giữ danh mục nữa thì có hai
 * danh mục, và chúng sẽ lệch nhau vào đúng ngày ai đó quên.
 *
 * Cái giá: không tầng nào cưỡng chế được giá trị hợp lệ. `khungCua()` là chỗ
 * nhận cái giá đó — giá trị lạ rơi về mặc định của loại thay vì làm trắng trang.
 * Gỡ một template mà sản phẩm cũ còn trỏ tới là chuyện **chắc chắn** xảy ra.
 */

/** Mặc định khi chưa ai chọn, và khi giá trị đã chọn không còn tồn tại. */
export const KHUNG_MAC_DINH = 'co-dien';

/**
 * Loại nào dùng được template nào.
 *
 * Loại không có mặt trong bảng này chỉ có `co-dien` — đó là trạng thái của năm
 * loại còn lại hôm nay, và nó đúng: `05` mục 7 và 8 đã tả riêng khung cho
 * `COMBO` và `DAY_TOUR`, nhưng chúng chưa dựng, nên liệt kê tên chúng ở đây là
 * hứa một thứ không có.
 */
export const KHUNG_THEO_LOAI: Record<string, readonly string[]> = {
  GROUP_TOUR: ['co-dien', 'tap-chi', 'ke-chuyen'],
};

export type Khung = 'co-dien' | 'tap-chi' | 'ke-chuyen';

/** Template nào hợp lệ cho loại này. Luôn có ít nhất `co-dien`. */
export function khungCoMat(productType: string): readonly string[] {
  return KHUNG_THEO_LOAI[productType] ?? [KHUNG_MAC_DINH];
}

/**
 * Giá trị trong CSDL thành template thật sự dùng.
 *
 * Không ném lỗi và không cảnh báo: `layout` vắng là trạng thái bình thường của
 * mọi sản phẩm chưa ai chạm tới, và một giá trị lạ chỉ có nghĩa là template đó
 * đã bị gỡ. Cả hai đều không phải lý do để một trang đang bán hàng hỏng.
 */
export function khungCua(productType: string, layout: string | undefined): Khung {
  const coMat = khungCoMat(productType);
  return (layout !== undefined && coMat.includes(layout) ? layout : KHUNG_MAC_DINH) as Khung;
}
