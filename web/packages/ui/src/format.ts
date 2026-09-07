import type { Locale } from '@travel/i18n';

/**
 * Tiền như API trả về. `amount` là **chuỗi**, không phải số.
 *
 * Số dấu phẩy động của JavaScript làm hỏng tiền, nên chuỗi đi thẳng từ API tới
 * hàm định dạng, không qua `parseFloat`, không qua phép tính nào.
 */
export interface Money {
  amount: string;
  currency: string;
}

const locales: Record<Locale, string> = { da: 'da-DK', vi: 'vi-VN' };

/**
 * Định dạng tiền. Đây là **hàm duy nhất** được phép làm việc này — đừng định
 * dạng rải rác trong component.
 *
 * Số chữ số thập phân KHÔNG hardcode: `Intl.NumberFormat` lấy từ mã tiền tệ,
 * nên DKK ra 2 chữ số và VND ra 0 mà không cần bảng tra nào.
 *
 * `format()` nhận thẳng chuỗi (Intl.NumberFormat V3) nên giữ nguyên độ chính
 * xác. Không `parseFloat` — CLAUDE.md của `web/` mục 11 cấm.
 */
export function formatMoney(money: Money, locale: Locale): string {
  return new Intl.NumberFormat(locales[locale], {
    style: 'currency',
    currency: money.currency,
  }).format(money.amount as unknown as number);
}

/**
 * Ngày: `14. marts 2027` ở `da`, `14/03/2027` ở `vi`.
 *
 * Nhận cả chuỗi ISO lẫn `Date`: client sinh từ spec đã đổi trường `format: date`
 * thành `Date` sẵn, còn chuỗi thì tới từ những chỗ chưa qua client.
 *
 * **Đọc ở múi giờ ĐỊA PHƯƠNG, và đó là cách duy nhất đúng ở đây** — không phải
 * một lựa chọn tuỳ tiện, mà là hệ quả của cách `Date` tới tay hàm này.
 *
 * Client sinh từ spec dựng trường `format: date` thành **nửa đêm giờ địa
 * phương** (xem `parseDate` trong runtime của nó): mốc đó biểu diễn đúng ngày
 * trên tờ lịch ở mọi múi giờ. Định dạng nó bằng `timeZone: 'UTC'` là đổi hệ quy
 * chiếu giữa chừng, và ở phía đông UTC thì nửa đêm địa phương rơi vào **hôm
 * trước** theo UTC — ngày 20/03 hiện thành 19/03 cho cả đội đang ngồi ở Việt
 * Nam. Đó là lỗi đã có thật, không phải giả định.
 *
 * Chuỗi cũng dựng thành nửa đêm địa phương, cùng quy ước — hai đường vào phải
 * cho cùng một kết quả, nếu không thì lỗi chỉ hiện ở một nửa số chỗ gọi.
 */
export function formatDate(iso: string | Date, locale: Locale): string {
  const ngay = iso instanceof Date ? iso : nuaDemDiaPhuong(iso);
  return new Intl.DateTimeFormat(locales[locale], {
    day: 'numeric',
    month: locale === 'da' ? 'long' : '2-digit',
    year: 'numeric',
  }).format(ngay);
}

/**
 * `new Date('2027-03-20')` là nửa đêm **UTC** — sai ngày ở phía tây UTC. Dựng
 * tay theo từng thành phần để ra nửa đêm địa phương, khớp `parseDate`.
 */
function nuaDemDiaPhuong(iso: string): Date {
  const chi = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!chi) {
    return new Date(iso);
  }
  return new Date(Number(chi[1]), Number(chi[2]) - 1, Number(chi[3]));
}

/** Số nguyên theo quy ước từng ngôn ngữ. Dùng cho số lượng, không cho tiền. */
export function formatNumber(value: number, locale: Locale): string {
  return new Intl.NumberFormat(locales[locale]).format(value);
}
