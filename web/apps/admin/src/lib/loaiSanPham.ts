/**
 * Sáu loại sản phẩm và bộ trường riêng của từng loại.
 *
 * Một chỗ duy nhất, vì ba màn hình cùng cần: danh sách lọc theo loại, màn tạo
 * dựng biểu mẫu theo loại, màn sửa nạp lại đúng bộ đó. Ba bản sao là ba cơ hội
 * để một bản quên mất một trường, và trường bị quên thì lặng lẽ không bao giờ
 * được ghi.
 *
 * Khớp sáu bảng con của `docs/12` mục 4.2 và sáu khối của `AdminProductCreate`.
 */

export const LOAI = [
  ['', 'Mọi loại'],
  ['GROUP_TOUR', 'Tour đoàn'],
  ['INDIVIDUAL_PACKAGE', 'Tour cá nhân'],
  ['PRIVATE_TOUR', 'Tour riêng'],
  ['CRUISE', 'Du thuyền'],
  ['COMBO', 'Combo bay + khách sạn'],
  ['DAY_TOUR', 'Tour trong ngày'],
] as const;

export function tenLoai(ma: string): string {
  return LOAI.find(([m]) => m === ma)?.[1] ?? ma;
}

/** `[tên trường, nhãn tiếng Việt, kiểu ô nhập]` */
export const TRUONG_THEO_LOAI: Record<
  string,
  ReadonlyArray<readonly [string, string, string]>
> = {
  GROUP_TOUR: [
    ['minPax', 'Số khách tối thiểu (10–25)', 'number'],
    ['maxPax', 'Số khách tối đa (10–25)', 'number'],
    ['guaranteedThreshold', 'Ngưỡng chắc chắn khởi hành', 'number'],
    ['tourLeaderLanguage', 'Ngôn ngữ trưởng đoàn — da hoặc vi', 'text'],
    ['fitnessLevel', 'Mức thể lực (1–4)', 'number'],
  ],
  INDIVIDUAL_PACKAGE: [
    ['minPartySize', 'Số khách tối thiểu', 'number'],
    ['flexibleDateWindowDays', 'Cửa sổ ngày linh hoạt (0–30 ngày)', 'number'],
  ],
  PRIVATE_TOUR: [
    ['leadTimeDays', 'Đặt trước tối thiểu (1–90 ngày)', 'number'],
    ['quoteValidDays', 'Báo giá có hiệu lực (1–30 ngày)', 'number'],
  ],
  CRUISE: [
    ['shipName', 'Tên tàu', 'text'],
    ['portCount', 'Số cảng ghé', 'number'],
  ],
  COMBO: [
    ['nights', 'Số đêm (1–14)', 'number'],
    ['validFrom', 'Hiệu lực từ — YYYY-MM-DD', 'text'],
    ['validTo', 'Hiệu lực đến — YYYY-MM-DD', 'text'],
  ],
  DAY_TOUR: [
    ['durationHours', 'Thời lượng (1–24 giờ)', 'number'],
    ['cutoffHours', 'Đóng bán trước (giờ)', 'number'],
  ],
};

const KHOI_THEO_LOAI: Record<string, string> = {
  GROUP_TOUR: 'groupTour',
  INDIVIDUAL_PACKAGE: 'individualPackage',
  PRIVATE_TOUR: 'privateTour',
  CRUISE: 'cruise',
  COMBO: 'combo',
  DAY_TOUR: 'dayTour',
};

/** Đọc khối riêng của loại từ một sản phẩm đã có, sang dạng chuỗi cho ô nhập. */
export function khoiBanDau(sp: object, productType: string): Record<string, string> {
  const ten = KHOI_THEO_LOAI[productType];
  const k = ten
    ? (sp as Record<string, Record<string, unknown> | undefined>)[ten]
    : undefined;
  if (!k) {
    return {};
  }
  return Object.fromEntries(Object.entries(k).map(([a, b]) => [a, String(b ?? '')]));
}

/**
 * Đóng gói khối riêng của loại để gửi lên.
 *
 * Thiếu một trường thì **không gửi khối nào**: `PATCH` bỏ qua khối vắng mặt, còn
 * gửi khối thiếu trường là chắc chắn `400 PRODUCT_TYPE_BLOCK_MISMATCH`.
 */
export function khoiGui(
  productType: string,
  gt: Record<string, string>,
): Record<string, unknown> {
  const ten = KHOI_THEO_LOAI[productType];
  const truong = TRUONG_THEO_LOAI[productType] ?? [];
  if (!ten || truong.length === 0 || truong.some(([t]) => !gt[t])) {
    return {};
  }
  const noiDung = Object.fromEntries(
    truong.map(([t, , kieu]) => [t, kieu === 'number' ? Number(gt[t]) : gt[t]]),
  );
  return { [ten]: noiDung };
}
