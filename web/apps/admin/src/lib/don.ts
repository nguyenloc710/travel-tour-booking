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

/**
 * Bốn thao tác nhân viên làm được, và hậu quả của từng cái.
 *
 * Bảng này **soi lại** máy trạng thái ở backend (`docs/23` mục 4) để giao diện
 * chỉ hiện nút bấm được. Nó KHÔNG thay thế phép kiểm ở backend: hai nhân viên
 * cùng mở một đơn thì bảng này đã cũ ở một trong hai màn hình, và câu trả lời
 * đúng lúc đó là `409` từ máy chủ chứ không phải một nút bị ẩn.
 *
 * `hauQua` là câu hiện trong ô xác nhận. `docs/22` mục 7: ô xác nhận phải nói rõ
 * hậu quả — "Huỷ đơn này và trả 2 chỗ về kho" khác hẳn "Bạn có chắc không?".
 */
const THAO_TAC: Record<string, ReadonlyArray<{ sang: string; nhan: string; nang: boolean }>> = {
  DRAFT: [{ sang: 'CANCELLED', nhan: 'Huỷ đơn', nang: true }],
  PENDING_PAYMENT: [
    { sang: 'CONFIRMED', nhan: 'Xác nhận đơn', nang: false },
    { sang: 'CANCELLED', nhan: 'Huỷ đơn', nang: true },
  ],
  PENDING_CONFIRMATION: [
    { sang: 'CONFIRMED', nhan: 'Xác nhận đơn', nang: false },
    { sang: 'CANCELLED', nhan: 'Huỷ đơn', nang: true },
  ],
  CONFIRMED: [
    { sang: 'COMPLETED', nhan: 'Đánh dấu đã đi', nang: false },
    { sang: 'CANCELLED', nhan: 'Huỷ đơn', nang: true },
  ],
  CANCELLED: [{ sang: 'REFUNDED', nhan: 'Đánh dấu đã hoàn tiền', nang: false }],
  COMPLETED: [],
  REFUNDED: [],
  EXPIRED: [],
};

export function thaoTacChoPhep(trangThai: string) {
  return THAO_TAC[trangThai] ?? [];
}

/** Câu mô tả hậu quả, dựng theo đúng đơn đang mở. */
export function hauQua(sang: string, soKhach: number): string {
  switch (sang) {
    case 'CANCELLED':
      return `Huỷ đơn này và trả ${soKhach} chỗ về kho ngay. Không đảo ngược được — máy trạng thái không có bước lùi, huỷ nhầm thì phải tạo đơn mới.`;
    case 'REFUNDED':
      return 'Ghi nhận đã hoàn tiền xong. Chỗ đã về kho từ lúc huỷ, thao tác này không đụng tới tồn kho.';
    case 'COMPLETED':
      return 'Đánh dấu khách đã đi xong. Đây là trạng thái cuối, không đi tiếp được nữa.';
    case 'CONFIRMED':
      return 'Xác nhận đơn chắc chắn đi. Chỗ vẫn giữ nguyên trong kho.';
    default:
      return 'Thao tác này ghi một dòng vào nhật ký của đơn.';
  }
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

/**
 * Nhãn một dòng giá.
 *
 * Dịch theo **`kind`**, không theo `labelKey` — cùng cách site khách đã làm
 * (`priceLine.<KIND>` trong catalog dùng chung), và chữ ở đây lấy đúng bản `vi`
 * của catalog đó để hai bề mặt không gọi cùng một dòng bằng hai cái tên.
 *
 * `kind` là enum sinh từ spec nên nó **đóng**; `labelKey` là chuỗi tự do mang
 * thêm mã loại khách (`price.base.ADULT`). Dòng `BASE` ghép thêm mã đó vì một
 * đơn có nhiều dòng `BASE`, mỗi loại khách một dòng — thiếu nó thì bảng hiện ba
 * dòng "Giá cơ bản" giống hệt nhau.
 *
 * Không khớp thì trả lại **nguyên khoá**: một nhãn thiếu phải nhìn thấy được,
 * chứ không được biến thành ô trống.
 */
const NHAN_DONG_GIA: Record<string, string> = {
  BASE: 'Giá cơ bản',
  SINGLE_SUPPLEMENT: 'Phụ thu phòng đơn',
  CABIN_UPGRADE: 'Nâng hạng cabin',
  DEPARTURE_ORIGIN: 'Điểm khởi hành',
  INSURANCE: 'Bảo hiểm du lịch',
  PRE_TOUR_HOTEL: 'Khách sạn trước tour',
  EARLY_BIRD_DISCOUNT: 'Giảm giá đặt sớm',
  PROCESSING_FEE: 'Phí xử lý',
};

/** Mã loại khách là dữ liệu RIÊNG TỪNG THỊ TRƯỜNG — mã lạ thì hiện nguyên mã. */
const NHAN_LOAI_KHACH: Record<string, string> = {
  ADULT: 'người lớn',
  CHILD: 'trẻ em',
  INFANT: 'em bé',
};

export function tenDongGia(kind: string, labelKey: string): string {
  const nhan = NHAN_DONG_GIA[kind];
  if (!nhan) {
    return labelKey;
  }
  if (kind !== 'BASE') {
    return nhan;
  }
  const ma = labelKey.startsWith('price.base.') ? labelKey.slice('price.base.'.length) : '';
  return ma ? `${nhan} — ${NHAN_LOAI_KHACH[ma] ?? ma}` : nhan;
}
