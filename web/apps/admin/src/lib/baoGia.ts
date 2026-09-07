/**
 * Nhãn và luật dùng chung của hai màn hình báo giá (docs/22 M8).
 *
 * Đặt ở `lib/` chứ không xuất từ một `page.tsx`, cùng lý do với `don.ts`: một
 * trang import từ trang khác là kéo theo cả cây component của trang đó vào
 * bundle.
 */

/** Vòng đời ở `docs/14` mục 7. Trang quản trị dùng một ngôn ngữ — `vi`. */
const TRANG_THAI: ReadonlyArray<readonly [string, string]> = [
  ['DRAFT', 'Chờ dựng giá'],
  ['SENT', 'Đã gửi khách'],
  ['ACCEPTED', 'Khách đã nhận'],
  ['REJECTED', 'Khách từ chối'],
  ['EXPIRED', 'Hết hạn'],
];

export const TRANG_THAI_CHON = TRANG_THAI;

export function tenTrangThai(ma: string): string {
  return TRANG_THAI.find(([m]) => m === ma)?.[1] ?? ma;
}

/**
 * Vàng = còn nợ khách một việc, xanh = đã chốt xuôi, xám = đã đóng.
 *
 * `SENT` vẫn là vàng dù bóng đang ở sân khách: tư vấn viên còn phải theo dõi nó
 * tới lúc hết hạn, và một báo giá gửi đi rồi quên mất là một đơn đã mất.
 */
export function mauTrangThai(ma: string): string {
  if (ma === 'ACCEPTED') {
    return 'xanh';
  }
  if (ma === 'REJECTED' || ma === 'EXPIRED') {
    return 'xam';
  }
  return 'vang';
}

/**
 * Thao tác nhân viên làm được ở từng trạng thái.
 *
 * Bảng này **soi lại** máy trạng thái ở backend để giao diện chỉ hiện nút bấm
 * được. Nó KHÔNG thay thế phép kiểm ở đó: hai tư vấn viên cùng mở một báo giá
 * thì bảng này đã cũ ở một trong hai màn hình, và câu trả lời đúng lúc đó là
 * `409` từ máy chủ chứ không phải một nút bị ẩn.
 *
 * `EXPIRED` không có trong bảng — nó do job sinh ra, không phải quyết định của
 * người (docs/14 mục 7).
 */
const THAO_TAC: Record<string, ReadonlyArray<{ sang: string; nhan: string; nang: boolean }>> = {
  DRAFT: [{ sang: 'SENT', nhan: 'Gửi cho khách', nang: false }],
  SENT: [
    { sang: 'ACCEPTED', nhan: 'Khách đã nhận', nang: false },
    { sang: 'REJECTED', nhan: 'Khách từ chối', nang: true },
  ],
  ACCEPTED: [],
  REJECTED: [],
  EXPIRED: [],
};

export function thaoTacChoPhep(trangThai: string) {
  return THAO_TAC[trangThai] ?? [];
}

/**
 * Câu mô tả hậu quả cho ô xác nhận — `docs/22` mục 7 đòi nói rõ hậu quả, không
 * hỏi "bạn có chắc không".
 */
export function hauQua(sang: string, soNgayHieuLuc: number): string {
  switch (sang) {
    case 'SENT':
      return `Gửi bảng giá này cho khách và khoá nó lại: sau bước này không sửa được dòng giá nào nữa, vì bảng giá lúc đó là thứ khách đang cầm trong tay. Hạn hiệu lực đặt thành ${soNgayHieuLuc} ngày kể từ hôm nay.`;
    case 'ACCEPTED':
      return 'Ghi nhận khách đồng ý với báo giá này. Đây là trạng thái cuối — không đi tiếp được nữa.';
    case 'REJECTED':
      return 'Ghi nhận khách từ chối. Không đảo ngược được; khách đổi ý thì gửi một yêu cầu mới.';
    default:
      return 'Thao tác này đổi trạng thái của báo giá.';
  }
}

/** Chỉ `DRAFT` còn sửa được bảng giá — soi lại `QuoteStatuses.suaBangGiaDuoc`. */
export function suaBangGiaDuoc(trangThai: string): boolean {
  return trangThai === 'DRAFT';
}

/**
 * Nhãn gợi ý cho dòng giá.
 *
 * `labelKey` là **khoá chuỗi**, không phải câu tiếng người: báo giá phải in ra
 * được ở cả hai ngôn ngữ, nên dòng của nó không mang sẵn tiếng nào. Bảng này
 * cho tư vấn viên chọn từ những khoá đã có nhãn thay vì gõ khoá tự do — gõ tay
 * là cách chắc chắn để sinh ra `line.guilde` và một dòng không dịch được.
 */
export const KHOA_DONG_GIA: ReadonlyArray<readonly [string, string]> = [
  ['quote.line.base', 'Giá tour cho mỗi khách'],
  ['quote.line.accommodation', 'Khách sạn'],
  ['quote.line.transport', 'Di chuyển'],
  ['quote.line.guide', 'Hướng dẫn viên riêng'],
  ['quote.line.meals', 'Ăn uống'],
  ['quote.line.activity', 'Hoạt động, vé tham quan'],
  ['quote.line.singleSupplement', 'Phụ thu phòng đơn'],
  ['quote.line.discount', 'Giảm trừ'],
  ['quote.line.processingFee', 'Phí xử lý'],
];

/**
 * Khoá của dòng mới thêm.
 *
 * Hằng riêng chứ không `KHOA_DONG_GIA[0][0]`: chỉ mục vào mảng trả kiểu có thể
 * `undefined` dưới `noUncheckedIndexedAccess`, và một hằng nói rõ ý hơn hẳn một
 * phép chỉ mục mà người đọc phải tự suy ra là "cái đầu tiên".
 */
export const KHOA_DONG_GIA_MAC_DINH = 'quote.line.base';

export function tenDongGia(labelKey: string): string {
  return KHOA_DONG_GIA.find(([k]) => k === labelKey)?.[1] ?? labelKey;
}

/**
 * Ngày trên **tờ lịch**, đọc ở giờ địa phương — cùng lý do với `don.ts`: client
 * dựng `format: date` thành nửa đêm địa phương, ép về UTC là lùi một ngày ở
 * phía đông UTC.
 */
export function ngay(d: Date | undefined | null): string {
  return d
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short' }).format(d)
    : '—';
}

/** Mốc thời gian thật thì hiện theo giờ của máy đang xem. */
export function ngayGio(d: Date | undefined | null): string {
  return d
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(d)
    : '—';
}

/**
 * Còn mấy ngày nữa hết hạn — số âm nghĩa là đã quá hạn.
 *
 * So hai ngày trên **tờ lịch địa phương**, không so hai mốc thời gian: chênh
 * lệch mili giây chia cho 86.400.000 sẽ lệch một đơn vị mỗi lần một trong hai
 * mốc rơi qua ranh giới ngày.
 */
export function conLai(validUntil: Date | undefined | null): number | null {
  if (!validUntil) {
    return null;
  }
  const homNay = new Date();
  const mocHomNay = Date.UTC(homNay.getFullYear(), homNay.getMonth(), homNay.getDate());
  const mocHan = Date.UTC(
    validUntil.getFullYear(),
    validUntil.getMonth(),
    validUntil.getDate(),
  );
  return Math.round((mocHan - mocHomNay) / 86_400_000);
}
