import { Configuration, ProductsApi, RegionsApi, ResponseError } from '@travel/api-client';
import type { Locale, Market } from '@travel/i18n';
import { marketSegment } from './market';

const BASE_PATH = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

/**
 * Client API sinh từ `contracts/openapi.yaml`.
 *
 * Không viết `fetch` tay tới API ở bất kỳ đâu — mọi truy vấn đi qua client sinh
 * ra, để đổi spec mà quên sửa chỗ gọi là lỗi biên dịch chứ không phải bug lúc
 * chạy (ADR-002).
 */
function configuration(): Configuration {
  return new Configuration({ basePath: BASE_PATH });
}

export function regionsApi(): RegionsApi {
  return new RegionsApi(configuration());
}

export function productsApi(): ProductsApi {
  return new ProductsApi(configuration());
}

/**
 * Phân biệt "không tìm thấy" với "gọi API hỏng".
 *
 * Hai thứ này dẫn tới hai màn hình khác nhau: 404 là trang không tồn tại, còn
 * lỗi mạng là trạng thái lỗi kèm số điện thoại. Gộp chúng lại thì một sự cố
 * backend hiện ra thành "trang này không tồn tại" và khách bỏ đi thật.
 */
export function laKhongTimThay(loi: unknown): boolean {
  return loi instanceof ResponseError && loi.response.status === 404;
}

/**
 * Hai tham số bắt buộc của mọi lời gọi API công khai.
 *
 * Market đi vào **đường dẫn** vì nó đổi tài nguyên; locale đi vào **header**
 * `Accept-Language` vì nó chỉ đổi cách trình bày. Không gộp, không đảo.
 */
export function requestScope(market: Market, locale: Locale) {
  return { market: marketSegment(market), acceptLanguage: locale };
}
