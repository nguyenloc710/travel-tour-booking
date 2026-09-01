import type { Locale } from './index';

/**
 * Ánh xạ đoạn đường dẫn theo locale — `docs/02` mục 5.1.
 *
 * **Cả đoạn đường dẫn lẫn slug đều dịch.** Không dùng đoạn tiếng Đan cho trang
 * tiếng Việt. Bảng này là dữ liệu tĩnh trong `web/`, KHÔNG nằm trong CSDL: nó
 * thuộc về cấu trúc site, không thuộc về nội dung.
 *
 * Đoạn đường dẫn không dùng ký tự có dấu ở cả hai ngôn ngữ — `bekraeftelse`
 * chứ không `bekræftelse`, `dat-tour` chứ không `đặt-tour`.
 */
export const pathnames = {
  home: { da: '', vi: '' },
  tourFinder: { da: 'rejsefinder', vi: 'tim-tour' },
  destinations: { da: 'destinationer', vi: 'diem-den' },
  products: { da: 'rejser', vi: 'tour' },
  booking: { da: 'booking', vi: 'dat-tour' },
  confirmation: { da: 'bekraeftelse', vi: 'xac-nhan' },
  blog: { da: 'blog', vi: 'blog' },
  contact: { da: 'kontakt', vi: 'lien-he' },
  events: { da: 'foredrag', vi: 'su-kien' },
} as const satisfies Record<string, Record<Locale, string>>;

export type PathKey = keyof typeof pathnames;

export function segmentFor(key: PathKey, locale: Locale): string {
  return pathnames[key][locale];
}
