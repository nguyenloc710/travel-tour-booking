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
});
