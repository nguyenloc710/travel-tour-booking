import { describe, expect, it } from 'vitest';
import { formatDate, formatMoney } from './format';

describe('định dạng tiền', () => {
  it('DKK ra 2 chữ số thập phân, VND ra 0 — lấy từ mã tiền tệ, không hardcode', () => {
    const dkk = formatMoney({ amount: '24990.00', currency: 'DKK' }, 'da');
    const vnd = formatMoney({ amount: '18900000', currency: 'VND' }, 'vi');

    expect(dkk).toContain('24');
    expect(dkk).toMatch(/,00|\.00/);
    expect(vnd).not.toMatch(/[.,]00\b/);
  });

  it('giữ nguyên độ chính xác của chuỗi, không đi qua parseFloat', () => {
    // 0.1 + 0.2 của JavaScript ra 0.30000000000000004. Chuỗi thì không.
    const ket_qua = formatMoney({ amount: '0.30', currency: 'DKK' }, 'da');
    expect(ket_qua).not.toContain('0000');
  });

  it('cùng số tiền, hai locale định dạng khác nhau', () => {
    const a = formatMoney({ amount: '1234.50', currency: 'DKK' }, 'da');
    const b = formatMoney({ amount: '1234.50', currency: 'DKK' }, 'vi');
    expect(a).not.toBe(b);
  });
});

describe('định dạng ngày', () => {
  it('da dùng tên tháng, vi dùng số', () => {
    expect(formatDate('2027-03-14', 'da')).toMatch(/marts/);
    expect(formatDate('2027-03-14', 'vi')).toMatch(/03/);
  });

  /**
   * Lỗi đã xảy ra thật: buổi thuyết trình ngày 20/03 hiện thành 19/03 trên máy
   * ở Việt Nam (UTC+7).
   *
   * Nguyên nhân là hai quy ước ngược nhau — client sinh từ spec dựng
   * `format: date` thành nửa đêm **địa phương**, còn hàm này định dạng ở
   * **UTC**. Nửa đêm địa phương phía đông UTC rơi vào hôm trước theo UTC.
   *
   * Bài test dựng `Date` đúng như client làm, nên nó đỏ nếu ai đó thêm lại
   * `timeZone: 'UTC'`.
   */
  it('giữ đúng ngày trên tờ lịch cho Date dựng ở nửa đêm địa phương', () => {
    const nuaDemDiaPhuong = new Date(2027, 2, 20);

    expect(formatDate(nuaDemDiaPhuong, 'vi')).toContain('20');
    expect(formatDate(nuaDemDiaPhuong, 'da')).toMatch(/^20\./);
  });

  it('chuỗi và Date cho cùng một ngày', () => {
    // Hai đường vào phải cho cùng kết quả, nếu không thì lỗi chỉ hiện ở một
    // nửa số chỗ gọi — và nửa còn lại vẫn xanh trong test.
    expect(formatDate('2027-03-20', 'vi')).toBe(formatDate(new Date(2027, 2, 20), 'vi'));
  });
});
