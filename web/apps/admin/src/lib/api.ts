import {
  AdminApi,
  Configuration,
  ResponseError,
  type ErrorCode,
  type FieldError,
  type FieldRule,
  type Middleware,
} from '@travel/api-client';

/**
 * Client API của trang quản trị.
 *
 * Không viết `fetch` tay tới API ở bất kỳ đâu — mọi lời gọi đi qua client sinh
 * từ `contracts/openapi.yaml`, để đổi spec mà quên sửa chỗ gọi là lỗi biên dịch
 * chứ không phải bug lúc chạy (ADR-002).
 *
 * `basePath` rỗng: trình duyệt gọi **cùng origin** rồi Next chuyển tiếp sang
 * API — lý do ở `next.config.ts`.
 */

/**
 * Thẻ CSRF đi từ cookie sang header.
 *
 * Spring đặt cookie `XSRF-TOKEN` (đọc được bằng JS, có chủ ý) và đòi cùng giá
 * trị đó quay lại ở header `X-XSRF-TOKEN`. Kẻ tấn công ở tên miền khác gửi được
 * cookie kèm theo yêu cầu, nhưng **không đọc được** nó để đặt vào header — đó là
 * toàn bộ nguyên lý.
 *
 * Cookie phiên thì ngược lại: `HttpOnly`, JS không đọc được, và cũng không cần.
 */
const csrf: Middleware = {
  async pre(context) {
    const the = document.cookie
      .split('; ')
      .find((c) => c.startsWith('XSRF-TOKEN='))
      ?.slice('XSRF-TOKEN='.length);

    if (!the) {
      return context;
    }
    return {
      ...context,
      init: {
        ...context.init,
        headers: { ...context.init.headers, 'X-XSRF-TOKEN': decodeURIComponent(the) },
      },
    };
  },
};

export function adminApi(): AdminApi {
  return new AdminApi(
    new Configuration({
      basePath: '',
      // Cùng origin nên cookie tự đi kèm; ghi rõ ra để người đọc sau không phải
      // đoán vì sao phiên đăng nhập hoạt động.
      credentials: 'same-origin',
      middleware: [csrf],
    }),
  );
}

/** Chưa đăng nhập, hoặc phiên đã hết hạn. */
export function laChuaDangNhap(loi: unknown): boolean {
  return loi instanceof ResponseError && loi.response.status === 401;
}

/** Đúng vai trò nhưng không được làm việc này — ma trận quyền docs/22 mục 2.1. */
export function laKhongDuQuyen(loi: unknown): boolean {
  return loi instanceof ResponseError && loi.response.status === 403;
}

type Params = Record<string, unknown>;

/** Thân lỗi đã đọc — đọc một lần, dùng cho cả câu tổng lẫn lỗi từng trường. */
type ThanLoi = { params: Params; fields: FieldError[]; traceId: string };

async function errorBody(
  loi: ResponseError,
): Promise<({ code: ErrorCode | '' } & ThanLoi) | null> {
  try {
    const than = await loi.response.clone().json();
    return {
      // Ép kiểu ở đúng ranh giới mạng. Hợp đồng nói `code` là `ErrorCode`, nhưng
      // thứ đi qua dây vẫn là chuỗi, và một máy chủ mới hơn có thể gửi mã mà bản
      // frontend này chưa biết — `ERROR_TEXT[code]` trả `undefined` ở đó và câu
      // chung đỡ lấy. Chặt ở bên trong, mềm ở ngoài rìa.
      code: String(than.code ?? '') as ErrorCode | '',
      params: than.params ?? {},
      fields: Array.isArray(than.fields) ? than.fields : [],
      traceId: String(than.traceId ?? ''),
    };
  } catch {
    return null;
  }
}

/**
 * Mã lỗi của API sang câu tiếng Việt.
 *
 * API trả mã lỗi kèm tham số, không trả câu tiếng người (docs/13 mục 5). Trang
 * quản trị chỉ có một ngôn ngữ nên bảng dịch nằm thẳng trong tệp này; website
 * khách thì đi qua message catalog vì nó song ngữ.
 *
 * **`Record<ErrorCode, …>` là chỗ bắt buộc phải đủ.** Thêm một mã vào enum
 * `ErrorCode` của `contracts/openapi.yaml` mà quên viết câu ở đây là **lỗi biên
 * dịch**. Trước khi có enum, mười mã đang lặng lẽ rơi xuống nhánh mặc định và
 * hiện nguyên mã ra cho nhân viên đọc.
 */
const ERROR_TEXT: Record<ErrorCode, (loi: ThanLoi) => string> = {
  // ---------------------------------------------------------- 400 dữ liệu vào
  VALIDATION_FAILED: ({ fields }) => {
    // Có `fields` thì câu tổng chỉ còn là dòng dẫn: chỗ sai đã được đánh dấu
    // ngay tại ô nhập, và bắt người dùng đọc lại danh sách ở cuối trang là bắt
    // họ làm việc của biểu mẫu.
    if (fields.length === 0) {
      return 'Dữ liệu nhập chưa hợp lệ.';
    }
    return fields.length === 1
      ? 'Một trường chưa hợp lệ — xem dòng được đánh dấu bên dưới.'
      : `${fields.length} trường chưa hợp lệ — xem các dòng được đánh dấu bên dưới.`;
  },
  UNSUPPORTED_LOCALE: () => 'Ngôn ngữ này chưa được bật cho thị trường đang chọn.',
  PRODUCT_TYPE_BLOCK_MISMATCH: ({ params }) =>
    `Thiếu hoặc sai khối riêng của loại sản phẩm — cần khối "${params.expectedBlock}".`,
  DURATION_DAYS_RULE_VIOLATED: () =>
    'Số ngày sai với loại sản phẩm: DAY_TOUR không có số ngày, loại khác thì bắt buộc.',
  CABIN_CATEGORY_NOT_ALLOWED: () => 'Hạng cabin chỉ dùng được với sản phẩm loại CRUISE.',
  UNKNOWN_PAX_TYPE: ({ params }) =>
    `Thị trường ${params.market} không có loại khách "${params.paxTypeCode}".`,
  PRICE_TIER_NOT_CONTIGUOUS: ({ params }) =>
    `Thang giá không liền mạch tại mốc ${params.atMinPax} khách.`,

  // ---------------------------------------------------------- 401 · 403 · 404
  UNAUTHENTICATED: () => 'Phiên đăng nhập đã hết hạn. Đăng nhập lại để tiếp tục.',
  FORBIDDEN: () => 'Vai trò của bạn không được làm việc này.',
  NOT_FOUND: () => 'Không tìm thấy.',

  // ------------------------------------------- 409 trạng thái hiện tại không cho
  DEPARTURE_SOLD_OUT: () => 'Ngày khởi hành đã hết chỗ.',
  SEAT_HOLD_EXPIRED: () => 'Lượt giữ chỗ đã hết hạn. Chọn lại ngày khởi hành.',
  DEPARTURE_CLOSED: () => 'Ngày khởi hành đã đóng bán.',
  QUOTE_EXPIRED: () =>
    'Báo giá đã quá hạn. Báo giá không tự gia hạn — khách muốn tiếp thì gửi yêu cầu mới.',
  QUOTE_NOT_ACCEPTABLE: ({ params }) =>
    `Báo giá đang ở trạng thái "${params.from}" nên không làm được việc này.`,
  CAPACITY_BELOW_BOOKED: ({ params }) =>
    `Đã bán ${params.seatsBooked} chỗ, không hạ sức chứa xuống ${params.capacity} được.`,
  BOOKING_TRANSITION_NOT_ALLOWED: ({ params }) =>
    `Đơn đang ở "${params.from}" nên không chuyển sang "${params.to}" được. Nhiều khả năng người khác vừa đổi — tải lại trang.`,
  PRODUCT_HAS_ACTIVE_BOOKINGS: ({ params }) =>
    `Còn ${params.activeBookings} đơn chưa kết thúc. Muốn ngừng bán thì tắt công tắc thị trường.`,
  SINGLE_PRICE_MISSING: ({ params }) => {
    // Ba dạng tham số, ba câu: cả sản phẩm (lúc bật bán), một ngày khởi hành
    // (lúc lưu bảng giá thiếu dòng), và một loại khách (lúc giá phòng đơn không
    // cao hơn phòng đôi).
    if (params.departureCount !== undefined) {
      return `Còn ${params.departureCount} ngày khởi hành chưa có giá phòng đơn, sớm nhất là ${params.firstDepartureDate}. Nhập nốt bảng giá rồi bật bán lại — thiếu dòng đó thì khách đi một mình đặt được ở giá chia đôi phòng.`;
    }
    if (params.paxTypeCode !== undefined) {
      return `Giá phòng đơn ${params.singleAmount} của loại khách "${params.paxTypeCode}" không cao hơn giá phòng đôi ${params.doubleAmount}. Phụ thu tính bằng hiệu hai số đó, nên như vậy là phụ thu bằng 0.`;
    }
    return 'Bảng giá của tour có lưu trú phải có dòng phòng đơn (SINGLE). Thiếu nó thì phụ thu phòng đơn bằng 0 và khách đi một mình trả giá chia đôi phòng.';
  },
  DESTINATION_IN_USE: ({ params }) =>
    `Còn ${params.productCount} sản phẩm trỏ tới điểm đến này. Gỡ chúng sang điểm đến khác trước — xoá bây giờ sẽ làm chúng biến mất khỏi website mà không có lỗi nào báo.`,
  LAST_ADMIN: () =>
    'Đây là quản trị viên đang bật cuối cùng. Gán quyền ADMIN cho người khác trước, nếu không sẽ không ai vào lại được màn hình này.',
  IDEMPOTENCY_KEY_REUSED: () =>
    'Khoá chống trùng này đã dùng cho một yêu cầu khác. Tải lại trang rồi làm lại từ đầu.',

  // ----------------------------------------------- 422 không bao giờ hợp lệ
  LEAD_TIME_NOT_MET: ({ params }) =>
    `Tour này cần báo trước ${params.leadTimeDays} ngày; ngày sớm nhất là ${params.earliestDate}.`,
  PRODUCT_NOT_QUOTABLE: () => 'Loại sản phẩm này đặt thẳng được, không đi qua luồng báo giá.',
  PRODUCT_NOT_BOOKABLE: () =>
    'Loại sản phẩm này không đặt trực tiếp được — khách phải đi qua yêu cầu báo giá.',
  PARTY_SIZE_OUT_OF_RANGE: () =>
    'Số khách nằm ngoài khoảng cho phép của sản phẩm hoặc ngoài thang giá.',

  // ------------------------------------------------------------------- 500
  INTERNAL_ERROR: ({ traceId }) =>
    traceId === ''
      ? 'Lỗi máy chủ.'
      : `Lỗi máy chủ. Đưa mã tra cứu ${traceId} cho người trực kỹ thuật.`,
};

/**
 * Một luật bị vi phạm, thành câu tiếng Việt.
 *
 * Câu dựng từ `params` chứ không viết cứng con số: đổi ràng buộc trong
 * `openapi.yaml` thì câu ở đây tự đúng theo, không phải nhớ sửa hai chỗ.
 */
const RULE_TEXT: Record<FieldRule, (p: Params) => string> = {
  NOT_NULL: () => 'Bắt buộc nhập.',
  SIZE: (p) => {
    if (p.min !== undefined && p.max !== undefined) {
      return `Cần từ ${p.min} tới ${p.max}.`;
    }
    if (p.min !== undefined) {
      return `Cần ít nhất ${p.min}.`;
    }
    if (p.max !== undefined) {
      return `Nhiều nhất ${p.max}.`;
    }
    return 'Số lượng không hợp lệ.';
  },
  MIN: (p) => `Nhỏ nhất là ${p.min}.`,
  MAX: (p) => `Lớn nhất là ${p.max}.`,
  PATTERN: () => 'Sai định dạng.',
  // Backend gặp ràng buộc chưa có trong bảng ánh xạ của nó thì gửi mã này. Vẫn
  // nói được trường nào sai, chỉ là chưa có câu riêng cho luật đó.
  INVALID: () => 'Giá trị không hợp lệ.',
};

function ruleSentence(field: FieldError): string {
  const viet = RULE_TEXT[field.code];
  return viet ? viet((field.params ?? {}) as Params) : 'Giá trị không hợp lệ.';
}

/**
 * Mã lỗi và tham số, cho màn hình nào cần **thêm ngữ cảnh của chính nó** vào
 * câu chung.
 *
 * Có đúng hai chỗ như vậy: đổi trạng thái đơn và đổi trạng thái báo giá. Cả hai
 * đều muốn nói thêm "nhiều khả năng người khác vừa đổi — tải lại trang", và cả
 * hai đều dịch được mã trạng thái sang tên tiếng Việt của riêng màn hình mình.
 * Bảng dịch chung không làm được việc đó vì nó không biết đang ở màn hình nào.
 *
 * Trước đây hai màn hình ấy tự đọc `response.clone().json()` — hai bản sao của
 * cùng một đoạn bóc thân lỗi. Chúng vẫn giữ câu riêng, nhưng thôi giữ chỗ bóc.
 */
export async function errorInfo(
  loi: unknown,
): Promise<{ code: ErrorCode | ''; params: Params } | null> {
  if (!(loi instanceof ResponseError)) {
    return null;
  }
  const than = await errorBody(loi);
  return than === null ? null : { code: than.code, params: than.params };
}

/**
 * Lỗi của TỪNG TRƯỜNG: đường dẫn trong thân yêu cầu → câu tiếng Việt.
 *
 * Đường dẫn giữ nguyên như API trả (`source.longDescription`, `groupTour.minPax`)
 * — biểu mẫu nào gọi thì tự biết đường dẫn đó ứng với ô nhập nào của mình.
 */
export async function fieldErrors(loi: unknown): Promise<Record<string, string>> {
  if (!(loi instanceof ResponseError)) {
    return {};
  }
  const than = await errorBody(loi);
  if (than === null || than.code !== 'VALIDATION_FAILED') {
    return {};
  }
  return Object.fromEntries(than.fields.map((field) => [field.path, ruleSentence(field)]));
}

export async function loiTiengViet(loi: unknown): Promise<string> {
  if (!(loi instanceof ResponseError)) {
    return 'Không gọi được máy chủ. Kiểm tra API có đang chạy không.';
  }

  const than = await errorBody(loi);
  if (than === null) {
    return `Máy chủ trả lỗi ${loi.response.status}.`;
  }

  const viet = than.code === '' ? undefined : ERROR_TEXT[than.code];
  return viet ? viet(than) : `Lỗi ${than.code || loi.response.status}.`;
}
