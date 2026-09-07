import { AdminApi, Configuration, ResponseError, type Middleware } from '@travel/api-client';

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

/**
 * Mã lỗi và tham số của API, dịch sang câu tiếng Việt **ở đây**.
 *
 * API trả mã lỗi kèm tham số, không trả câu tiếng người (docs/13 mục 5). Chỗ
 * dịch là frontend, và với trang quản trị thì chỉ có một ngôn ngữ nên bảng dịch
 * nằm thẳng trong tệp này.
 */
export async function loiTiengViet(loi: unknown): Promise<string> {
  if (!(loi instanceof ResponseError)) {
    return 'Không gọi được máy chủ. Kiểm tra API có đang chạy không.';
  }

  let ma = '';
  let params: Record<string, unknown> = {};
  try {
    const than = await loi.response.clone().json();
    ma = String(than.code ?? '');
    params = than.params ?? {};
  } catch {
    return `Máy chủ trả lỗi ${loi.response.status}.`;
  }

  switch (ma) {
    case 'PRODUCT_TYPE_BLOCK_MISMATCH':
      return `Thiếu hoặc sai khối riêng của loại sản phẩm — cần khối "${params.expectedBlock}".`;
    case 'DURATION_DAYS_RULE_VIOLATED':
      return 'Số ngày sai với loại sản phẩm: DAY_TOUR không có số ngày, loại khác thì bắt buộc.';
    case 'CABIN_CATEGORY_NOT_ALLOWED':
      return 'Hạng cabin chỉ dùng được với sản phẩm loại CRUISE.';
    case 'UNKNOWN_PAX_TYPE':
      return `Thị trường ${params.market} không có loại khách "${params.paxTypeCode}".`;
    case 'PRICE_TIER_NOT_CONTIGUOUS':
      return `Thang giá không liền mạch tại mốc ${params.atMinPax} khách.`;
    case 'CAPACITY_BELOW_BOOKED':
      return `Đã bán ${params.seatsBooked} chỗ, không hạ sức chứa xuống ${params.capacity} được.`;
    case 'PRODUCT_HAS_ACTIVE_BOOKINGS':
      return `Còn ${params.activeBookings} đơn chưa kết thúc. Muốn ngừng bán thì tắt công tắc thị trường.`;
    case 'SINGLE_PRICE_MISSING':
      // Ba dạng tham số, ba câu: cả sản phẩm (lúc bật bán), một ngày khởi hành
      // (lúc lưu bảng giá thiếu dòng), và một loại khách (lúc giá phòng đơn
      // không cao hơn phòng đôi).
      if (params.departureCount !== undefined) {
        return `Còn ${params.departureCount} ngày khởi hành chưa có giá phòng đơn, sớm nhất là ${params.firstDepartureDate}. Nhập nốt bảng giá rồi bật bán lại — thiếu dòng đó thì khách đi một mình đặt được ở giá chia đôi phòng.`;
      }
      if (params.paxTypeCode !== undefined) {
        return `Giá phòng đơn ${params.singleAmount} của loại khách "${params.paxTypeCode}" không cao hơn giá phòng đôi ${params.doubleAmount}. Phụ thu tính bằng hiệu hai số đó, nên như vậy là phụ thu bằng 0.`;
      }
      return 'Bảng giá của tour có lưu trú phải có dòng phòng đơn (SINGLE). Thiếu nó thì phụ thu phòng đơn bằng 0 và khách đi một mình trả giá chia đôi phòng.';
    case 'QUOTE_EXPIRED':
      return 'Báo giá đã quá hạn. Báo giá không tự gia hạn — khách muốn tiếp thì gửi yêu cầu mới.';
    case 'QUOTE_NOT_ACCEPTABLE':
      return `Báo giá đang ở trạng thái "${params.from}" nên không làm được việc này.`;
    case 'PRODUCT_NOT_QUOTABLE':
      return 'Loại sản phẩm này đặt thẳng được, không đi qua luồng báo giá.';
    case 'LEAD_TIME_NOT_MET':
      return `Tour này cần báo trước ${params.leadTimeDays} ngày; ngày sớm nhất là ${params.earliestDate}.`;
    case 'DESTINATION_IN_USE':
      return `Còn ${params.productCount} sản phẩm trỏ tới điểm đến này. Gỡ chúng sang điểm đến khác trước — xoá bây giờ sẽ làm chúng biến mất khỏi website mà không có lỗi nào báo.`;
    case 'LAST_ADMIN':
      return 'Đây là quản trị viên đang bật cuối cùng. Gán quyền ADMIN cho người khác trước, nếu không sẽ không ai vào lại được màn hình này.';
    case 'VALIDATION_FAILED':
      return 'Dữ liệu nhập chưa hợp lệ.';
    case 'FORBIDDEN':
      return 'Vai trò của bạn không được làm việc này.';
    case 'NOT_FOUND':
      return 'Không tìm thấy.';
    default:
      return `Lỗi ${ma || loi.response.status}.`;
  }
}
