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
 * Luôn đọc ở múi giờ UTC. Ngày khởi hành là một ngày trên tờ lịch, không phải
 * một thời điểm — để trình duyệt áp múi giờ địa phương vào thì khách ở
 * Copenhagen và khách ở Hà Nội thấy hai ngày khác nhau cho cùng một chuyến.
 */
export function formatDate(iso: string | Date, locale: Locale): string {
  const ngay = iso instanceof Date ? iso : new Date(`${iso}T00:00:00Z`);
  return new Intl.DateTimeFormat(locales[locale], {
    day: 'numeric',
    month: locale === 'da' ? 'long' : '2-digit',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(ngay);
}

/** Số nguyên theo quy ước từng ngôn ngữ. Dùng cho số lượng, không cho tiền. */
export function formatNumber(value: number, locale: Locale): string {
  return new Intl.NumberFormat(locales[locale]).format(value);
}
