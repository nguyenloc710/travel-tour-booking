import type { AdminContentLocaleState } from '@travel/api-client';

/**
 * Nhãn dùng chung của màn hình nội dung khác (docs/22 M13).
 *
 * Đặt ở `lib/` chứ không xuất từ một `page.tsx`, cùng lý do với `don.ts` và
 * `baoGia.ts`: một trang import từ trang khác là kéo theo cả cây component của
 * trang đó vào bundle.
 */

/** Ba loại nội dung của M13 có đường ghi. Đoạn đường dẫn nằm luôn ở đây. */
export const LOAI_NOI_DUNG = [
  ['diem-den', 'Điểm đến'],
  ['bai-viet', 'Bài viết'],
  ['su-kien', 'Sự kiện'],
] as const;

export type LoaiNoiDung = (typeof LOAI_NOI_DUNG)[number][0];

export function laLoaiNoiDung(v: string): v is LoaiNoiDung {
  return LOAI_NOI_DUNG.some(([ma]) => ma === v);
}

/**
 * Trạng thái một locale, nói bằng câu người vận hành hiểu.
 *
 * `MISSING` **không** dịch thành "chưa dịch" cho xong: nó nghĩa là bản ghi này
 * *không tồn tại* với khách đọc thứ tiếng đó — chính sách không-fallback, và đó
 * là hậu quả người biên tập cần thấy chứ không phải một ô trống.
 */
const TRANG_THAI_LOCALE: Record<AdminContentLocaleState, readonly [string, string]> = {
  MISSING: ['Chưa có', 'xam'],
  DRAFT: ['Nháp', 'vang'],
  TRANSLATED: ['Đã dịch', 'vang'],
  PUBLISHED: ['Đã xuất bản', 'xanh'],
};

export function tenTrangThaiLocale(t: AdminContentLocaleState): string {
  return TRANG_THAI_LOCALE[t]?.[0] ?? t;
}

export function mauTrangThaiLocale(t: AdminContentLocaleState): string {
  return TRANG_THAI_LOCALE[t]?.[1] ?? 'xam';
}

/**
 * Hai locale của hệ thống, theo đúng thứ tự nguồn trước.
 *
 * Kiểu suy ra từ đây là `'da' | 'vi'`, khớp enum mà bộ sinh mã dựng cho tham số
 * `locale` — nên truyền thẳng được, không phải ép kiểu ở từng chỗ gọi.
 */
export const LOCALES = ['da', 'vi'] as const;

export type LocaleMa = (typeof LOCALES)[number];

/** Ba giá trị của cột `status` ở bản dịch bài viết. */
export const TRANG_THAI_BAN_DICH = [
  ['DRAFT', 'Nháp — khách không thấy'],
  ['TRANSLATED', 'Đã dịch — vẫn chưa lên website'],
  ['PUBLISHED', 'Xuất bản — khách đọc được'],
] as const;

/** Bốn vai trò của docs/22 mục 2. */
export const VAI_TRO = [
  ['CONSULTANT', 'Tư vấn viên', 'Xem đơn, dựng báo giá, xử lý yêu cầu tư vấn'],
  ['EDITOR', 'Biên tập', 'Viết nội dung bản da, xuất bản bản dịch'],
  ['TRANSLATOR', 'Người dịch', 'Dịch da → vi'],
  ['ADMIN', 'Quản trị', 'Toàn quyền, gồm giá và người dùng'],
] as const;

/** Ngày trên tờ lịch, đọc ở giờ địa phương — cùng lý do với `don.ts`. */
export function ngay(d: Date | undefined | null): string {
  return d ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short' }).format(d) : '—';
}

export function ngayGio(d: Date | undefined | null): string {
  return d
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(d)
    : '—';
}

/**
 * "Sửa lần cuối bởi ai, lúc nào" — `docs/22` mục 7 đòi hiện nó trên **mọi** bản
 * ghi: năm cột kiểm toán đã thu thập sẵn, không hiện ra thì thu thập làm gì.
 */
export function suaLanCuoi(luc: Date | undefined | null, boi: string | undefined): string {
  if (!luc) {
    return 'Chưa có lần sửa nào được ghi.';
  }
  return `Sửa lần cuối ${ngayGio(luc)}${boi ? ` bởi ${boi}` : ''}.`;
}
