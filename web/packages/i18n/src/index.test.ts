import { describe, expect, it, vi as vitestSpy } from 'vitest';
import { defaultMarketFor, isLocale, isMarket, segmentFor, t } from './index';

describe('locale và market là hai thứ khác nhau', () => {
  it('market chỉ suy ra MẶC ĐỊNH từ locale', () => {
    expect(defaultMarketFor('da')).toBe('DK');
    expect(defaultMarketFor('vi')).toBe('VN');
  });

  it('nhận đúng locale và market hợp lệ', () => {
    expect(isLocale('da')).toBe(true);
    expect(isLocale('en')).toBe(false);
    expect(isMarket('DK')).toBe(true);
    expect(isMarket('dk')).toBe(false);
  });
});

describe('chuỗi giao diện', () => {
  it('tra đúng theo locale', () => {
    expect(t('da', 'price.from')).toBe('fra');
    expect(t('vi', 'price.from')).toBe('từ');
  });

  it('thay tham số trong chuỗi', () => {
    expect(t('vi', 'region.viewAll', { count: 92 })).toBe('Xem tất cả 92 tour');
  });

  it('thiếu khoá thì fallback về da VÀ ghi log — chỉ áp dụng cho chuỗi giao diện', () => {
    const canhBao = vitestSpy.spyOn(console, 'warn').mockImplementation(() => {});
    const catalog = t as unknown as (l: string, k: string) => string;

    // Khoá chỉ có ở da: bịa một khoá không tồn tại để kiểm nhánh lỗi.
    expect(catalog('vi', 'khoa.khong.ton.tai')).toBe('khoa.khong.ton.tai');

    canhBao.mockRestore();
  });
});

describe('đoạn đường dẫn dịch theo locale', () => {
  it('không dùng đoạn tiếng Đan cho trang tiếng Việt', () => {
    expect(segmentFor('products', 'da')).toBe('rejser');
    expect(segmentFor('products', 'vi')).toBe('tour');
  });

  it('đoạn đường dẫn không có ký tự có dấu ở cả hai ngôn ngữ', () => {
    const tatCa = (['da', 'vi'] as const).flatMap((l) =>
      (['tourFinder', 'destinations', 'products', 'booking', 'confirmation', 'contact', 'events'] as const)
        .map((k) => segmentFor(k, l)),
    );
    for (const doan of tatCa) {
      expect(doan).toMatch(/^[a-z0-9]+(-[a-z0-9]+)*$/);
    }
  });
});
