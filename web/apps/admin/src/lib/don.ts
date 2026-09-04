/**
 * Nhãn và định dạng dùng chung của hai màn hình đơn (docs/22 M6 và M7).
 *
 * Đặt ở `lib/` chứ không xuất từ một `page.tsx`: một trang import từ trang khác
 * là kéo theo cả cây component của trang đó vào bundle.
 */

/** Máy trạng thái ở `docs/23` mục 4. Trang quản trị dùng một ngôn ngữ — `vi`. */
const TRANG_THAI: ReadonlyArray<readonly [string, string]> = [
  ['DRAFT', 'Nháp'],
  ['PENDING_PAYMENT', 'Chờ thanh toán'],
  ['PENDING_CONFIRMATION', 'Chờ xác nhận'],
  ['CONFIRMED', 'Đã xác nhận'],
  ['COMPLETED', 'Đã hoàn tất'],
  ['CANCELLED', 'Đã huỷ'],
  ['REFUNDED', 'Đã hoàn tiền'],
  ['EXPIRED', 'Hết hạn'],
];

export const TRANG_THAI_CHON = TRANG_THAI;

export function tenTrangThai(ma: string): string {
  return TRANG_THAI.find(([m]) => m === ma)?.[1] ?? ma;
}

/**
 * Xanh = xong việc của nhân viên, xám = đơn đã đóng, vàng = còn nợ khách một
 * hành động. Ba trạng thái vàng chính là `NEEDS_ACTION` của backend.
 */
export function mauTrangThai(ma: string): string {
  if (ma === 'CONFIRMED' || ma === 'COMPLETED') {
    return 'xanh';
  }
  if (ma === 'CANCELLED' || ma === 'EXPIRED' || ma === 'REFUNDED') {
    return 'xam';
  }
  return 'vang';
}

/** Ai gây ra một dòng nhật ký. */
export function tenTacNhan(loai: string): string {
  switch (loai) {
    case 'CUSTOMER':
      return 'Khách';
    case 'STAFF':
      return 'Nhân viên';
    case 'SYSTEM':
      return 'Hệ thống';
    default:
      return loai;
  }
}

/**
 * Ngày khởi hành là một ngày trên **tờ lịch**, không phải một thời điểm — đọc ở
 * UTC để nhân viên ở múi giờ nào cũng thấy đúng ngày ghi trên vé.
 */
export function ngay(d: Date | undefined | null): string {
  return d
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeZone: 'UTC' }).format(d)
    : '—';
}

/** Mốc thời gian thật thì ngược lại: hiện theo giờ của máy đang xem. */
export function ngayGio(d: Date | undefined | null): string {
  return d
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(d)
    : '—';
}
