import type { Route } from 'next';
import type { Locale } from '@travel/i18n';

/**
 * Tab của trang chi tiết sản phẩm — `docs/05` mục 3.
 *
 * **Slug của tab cũng dịch**, đúng luật của mọi đoạn URL trong dự án này
 * (`docs/02` mục 5.1): `?tab=dagsprogram` ở `da`, `?tab=lich-trinh` ở `vi`.
 * Không dùng một mã chung tiếng Anh cho cả hai — URL của mỗi ngôn ngữ phải đọc
 * được bằng chính ngôn ngữ đó, và tab đang mở nằm trong URL nên nó là URL thật
 * mà khách dán cho nhau.
 *
 * Slug lấy nguyên từ bảng ở `docs/05` mục 3, và **không có ký tự có dấu** ở cả
 * hai ngôn ngữ (`CLAUDE.md` quy tắc 7).
 */
export const TABS = {
  tongQuan: { da: 'oversigt', vi: 'tong-quan' },
  lichTrinh: { da: 'dagsprogram', vi: 'lich-trinh' },
  khachSan: { da: 'hoteller', vi: 'khach-san' },
  giaVaNgay: { da: 'priser-og-datoer', vi: 'gia-va-ngay' },
  thongTinThucTe: { da: 'praktisk-info', vi: 'thong-tin-thuc-te' },
} as const satisfies Record<string, Record<Locale, string>>;

export type TabKey = keyof typeof TABS;

export function slugTab(key: TabKey, locale: Locale): string {
  return TABS[key][locale];
}

/** Đoạn `?tab=` nhận được thuộc tab nào — không khớp thì về tổng quan. */
export function tabTuSlug(slug: string | undefined, locale: Locale): TabKey {
  const khop = (Object.keys(TABS) as TabKey[]).find((k) => TABS[k][locale] === slug);
  return khop ?? 'tongQuan';
}

/**
 * Tab nào có mặt, theo loại sản phẩm — `docs/05` mục 3.
 *
 * Bảng này nói **đủ những gì dựng được hôm nay**, không phải đủ những gì tài
 * liệu mô tả. Ba tab của `docs/05` còn thiếu và lý do:
 *
 * | Tab | Thiếu gì |
 * |---|---|
 * | Tàu và cabin (`CRUISE`) | `GET /products/{slug}/ship` chưa có trong hợp đồng |
 * | Bảng giá nhóm (`PRIVATE_TOUR`) | `GET /products/{slug}/price-tiers` công khai chưa có |
 * | Trưởng đoàn · Thời tiết | Chưa có bảng nào trong lược đồ |
 *
 * Hiện một tab rỗng để cho đủ số thì tệ hơn không hiện: khách bấm vào và không
 * thấy gì, còn đội ngũ thì tưởng phần đó đã xong.
 */
export function tabCoMat(productType: string): TabKey[] {
  const tabs: TabKey[] = ['tongQuan'];

  // `COMBO` và `DAY_TOUR` không có lịch trình theo ngày — endpoint trả 404
  // (`docs/13` mục 9.1), nên đừng hiện tab dẫn tới một lần gọi hỏng.
  if (productType !== 'COMBO' && productType !== 'DAY_TOUR') {
    tabs.push('lichTrinh');
  }
  // `CRUISE` ngủ trên tàu, không có chặng khách sạn.
  if (productType !== 'CRUISE') {
    tabs.push('khachSan');
  }
  // `PRIVATE_TOUR` không có ngày khởi hành cố định: khách chọn ngày trong yêu
  // cầu báo giá. Hiện bảng "giá và ngày" cho loại này là hứa một thứ không có.
  if (productType !== 'PRIVATE_TOUR') {
    tabs.push('giaVaNgay');
  }
  tabs.push('thongTinThucTe');
  return tabs;
}

/** Đường dẫn tới một tab của trang chi tiết. */
export function duongDanTab(
  locale: Locale,
  doanSanPham: string,
  slug: string,
  tab: TabKey,
  neo?: string,
): Route {
  const goc = `/${locale}/${doanSanPham}/${slug}`;
  // Tab tổng quan là mặc định nên URL của nó KHÔNG mang tham số: một trang có
  // hai URL cùng nội dung là nội dung trùng lặp với công cụ tìm kiếm, và
  // canonical của trang này trỏ về bản không tham số.
  const truyVan = tab === 'tongQuan' ? '' : `?tab=${slugTab(tab, locale)}`;
  return `${goc}${truyVan}${neo ?? ''}` as Route;
}
