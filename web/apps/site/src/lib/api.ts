import {
  BookingApi,
  Configuration,
  DestinationsApi,
  LecturesApi,
  PostsApi,
  ProductsApi,
  RegionsApi,
  ResponseError,
} from '@travel/api-client';
import { t, type Locale, type Market } from '@travel/i18n';
import { marketSegment } from './market-segment';

// `||` chứ không `??`: biến này bị nướng vào mã lúc build, và một `--build-arg`
// bỏ trống nướng vào chuỗi rỗng chứ không phải `undefined`. `??` sẽ để lọt
// `basePath: ''`, khiến mọi lời gọi API thành đường dẫn tương đối trỏ về chính
// máy chủ Next — hỏng im lặng, không có lỗi nào lúc build.
const BASE_PATH = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

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

export function destinationsApi(): DestinationsApi {
  return new DestinationsApi(configuration());
}

export function postsApi(): PostsApi {
  return new PostsApi(configuration());
}

export function lecturesApi(): LecturesApi {
  return new LecturesApi(configuration());
}

/**
 * Đường **ghi** của khách: tính giá, giữ chỗ, tạo đơn, tra cứu.
 *
 * Gọi từ trình duyệt chứ không từ máy chủ Next, khác mọi lời gọi đọc ở trên: giữ
 * chỗ và tạo đơn là hành động của khách, và chúng cần chạy ngay khi khách bấm.
 */
export function bookingApi(): BookingApi {
  return new BookingApi(configuration());
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
 * Mã lỗi của API → câu tiếng người của locale đang xem (docs/13 mục 5).
 *
 * **Một chỗ duy nhất.** Trước đây mỗi biểu mẫu mang một bản sao, và hai bản đã
 * kịp lệch nhau: bản trong `DatTour` bỏ qua `params`, nên khoá nào có chỗ thay
 * thế — `{leadTimeDays}`, `{earliestDate}` — sẽ hiện ra nguyên dấu ngoặc nếu nó
 * rơi vào màn hình đó.
 *
 * Mã chưa có khoá dịch thì hiện câu chung, **không hiện mã ra khách**
 * (`web/CLAUDE.md` mục 4). Đó là chủ ý, không phải thiếu sót: danh mục mã của
 * `ErrorCode` phục vụ cả bề mặt quản trị, và phần lớn mã ở đó không bao giờ
 * tới được mắt khách.
 */
export async function translateError(locale: Locale, loi: unknown): Promise<string> {
  let code = '';
  let params: Record<string, string | number> = {};

  if (loi instanceof ResponseError) {
    try {
      const than = await loi.response.clone().json();
      code = String(than.code ?? '');
      params = { ...((than.params ?? {}) as Record<string, string | number>) };
      // `traceId` nằm NGOÀI `params` trong hợp đồng, nhưng câu dịch của
      // INTERNAL_ERROR cần tới nó: khách đọc mã đó qua điện thoại cho tổng đài,
      // tổng đài tra log (docs/13 mục 5).
      if (than.traceId) {
        params.traceId = String(than.traceId);
      }
    } catch {
      code = '';
    }
  }

  const khoa = `error.${code}`;
  const cau = t(locale, khoa, params);
  return code !== '' && cau !== khoa ? cau : t(locale, 'error.generic');
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
