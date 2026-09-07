import type { Market } from '@travel/i18n';

/**
 * Market chữ thường khi đi vào đường dẫn API — api/CLAUDE.md mục 10.
 *
 * Kiểu trả về là union chữ thường chứ không phải `string`, để khớp thẳng với
 * enum sinh từ spec: thêm một market vào `openapi.yaml` mà quên thêm ở đây là
 * lỗi biên dịch.
 *
 * **Tách khỏi `market.ts` vì đây là hàm thuần.** `market.ts` đọc cookie qua
 * `next/headers`, thứ chỉ chạy được ở phía máy chủ — và luồng đặt tour là
 * component client, nên nó không được kéo theo `next/headers` chỉ để hạ một
 * chuỗi xuống chữ thường.
 */
export function marketSegment(market: Market): Lowercase<Market> {
  return market.toLowerCase() as Lowercase<Market>;
}
