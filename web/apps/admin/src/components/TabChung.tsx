'use client';

import { useState } from 'react';
import type { AdminProductDetail, AdminProductPatch } from '@travel/api-client';
import { adminApi, loiTiengViet } from '@/lib/api';

/**
 * Thông tin chung và phần riêng của loại (docs/22 M3).
 *
 * **Không có ô đổi loại sản phẩm, và đó là chủ ý.** Mỗi loại một bảng con; đổi
 * loại là mất dữ liệu riêng của loại cũ (docs/22 mục 7). Cần đổi thì tạo sản
 * phẩm mới. Hợp đồng API cũng không có trường đó, nên đây không phải chuyện giao
 * diện tự giới hạn.
 */
export function TabChung({
  sp,
  napLai,
}: {
  sp: AdminProductDetail;
  napLai: () => Promise<void>;
}) {
  const [heroImage, setHeroImage] = useState(sp.heroImage);
  const [mapImage, setMapImage] = useState(sp.mapImage ?? '');
  const [durationDays, setDurationDays] = useState(String(sp.durationDays ?? ''));
  const [isNew, setIsNew] = useState(sp.isNew);
  const [khoi, setKhoi] = useState<Record<string, string>>(khoiBanDau(sp));
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState('');
  const [dangLuu, setDangLuu] = useState(false);

  const truong = TRUONG_THEO_LOAI[sp.productType] ?? [];

  async function luu() {
    setLoi('');
    setXong('');
    setDangLuu(true);
    try {
      const than: AdminProductPatch = {
        heroImage,
        mapImage: mapImage || undefined,
        durationDays: durationDays ? Number(durationDays) : undefined,
        isNew,
        ...khoiGui(sp.productType, khoi),
      };
      await adminApi().suaSanPham({ id: sp.id, adminProductPatch: than });
      setXong('Đã lưu.');
      await napLai();
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  return (
    <>
      <h2>Thông tin chung</h2>

      <label htmlFor="loai">Loại sản phẩm</label>
      <input id="loai" value={sp.productType} readOnly className="chi-doc" />
      <p className="phu" style={{ marginTop: '0.25rem' }}>
        Không đổi được sau khi tạo. Mỗi loại một bộ trường riêng; đổi loại là mất
        dữ liệu của loại cũ.
      </p>

      <label htmlFor="anh">Ảnh đầu trang</label>
      <input id="anh" value={heroImage} onChange={(e) => setHeroImage(e.target.value)} />

      <label htmlFor="ban-do">Ảnh bản đồ</label>
      <input id="ban-do" value={mapImage} onChange={(e) => setMapImage(e.target.value)} />
      <p className="phu" style={{ marginTop: '0.25rem' }}>
        Hiện là đường dẫn nhập tay. Tải ảnh lên chưa có — chờ ADR-008 chốt nơi lưu.
      </p>

      {sp.productType !== 'DAY_TOUR' && (
        <>
          <label htmlFor="so-ngay">Số ngày</label>
          <input
            id="so-ngay"
            type="number"
            min={1}
            max={60}
            value={durationDays}
            onChange={(e) => setDurationDays(e.target.value)}
            style={{ maxWidth: '8rem' }}
          />
        </>
      )}

      <label htmlFor="moi" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
        <input
          id="moi"
          type="checkbox"
          checked={isNew}
          onChange={(e) => setIsNew(e.target.checked)}
        />
        Đánh dấu là tour mới
      </label>

      {truong.length > 0 && (
        <>
          <h2>Phần riêng của {sp.productType}</h2>
          {truong.map(([ten, nhan, kieu]) => (
            <div key={ten}>
              <label htmlFor={ten}>{nhan}</label>
              <input
                id={ten}
                type={kieu === 'number' ? 'number' : 'text'}
                value={khoi[ten] ?? ''}
                onChange={(e) => setKhoi({ ...khoi, [ten]: e.target.value })}
                style={{ maxWidth: kieu === 'number' ? '10rem' : '24rem' }}
              />
            </div>
          ))}
        </>
      )}

      {loi && <p className="loi">{loi}</p>}
      {xong && <p className="xong">{xong}</p>}

      <p style={{ marginTop: '1rem' }}>
        <button disabled={dangLuu} onClick={luu}>
          {dangLuu ? 'Đang lưu…' : 'Lưu'}
        </button>
      </p>
    </>
  );
}

/**
 * Trường riêng của từng loại — sáu bộ, khớp sáu bảng con của docs/12 mục 4.2.
 *
 * Gửi thiếu hoặc gửi sai bộ thì máy chủ trả `PRODUCT_TYPE_BLOCK_MISMATCH` kèm
 * tên khối cần có; giao diện chọn đúng bộ theo `productType` nên chuyện đó không
 * xảy ra từ đây.
 */
const TRUONG_THEO_LOAI: Record<string, ReadonlyArray<readonly [string, string, string]>> = {
  GROUP_TOUR: [
    ['minPax', 'Số khách tối thiểu', 'number'],
    ['maxPax', 'Số khách tối đa', 'number'],
    ['guaranteedThreshold', 'Ngưỡng chắc chắn khởi hành', 'number'],
    ['tourLeaderLanguage', 'Ngôn ngữ trưởng đoàn (da hoặc vi)', 'text'],
    ['fitnessLevel', 'Mức thể lực (1–4)', 'number'],
  ],
  INDIVIDUAL_PACKAGE: [
    ['minPartySize', 'Số khách tối thiểu', 'number'],
    ['flexibleDateWindowDays', 'Cửa sổ ngày linh hoạt (ngày)', 'number'],
  ],
  PRIVATE_TOUR: [
    ['leadTimeDays', 'Đặt trước tối thiểu (ngày)', 'number'],
    ['quoteValidDays', 'Báo giá có hiệu lực (ngày)', 'number'],
  ],
  CRUISE: [
    ['shipName', 'Tên tàu', 'text'],
    ['portCount', 'Số cảng ghé', 'number'],
  ],
  COMBO: [
    ['nights', 'Số đêm', 'number'],
    ['validFrom', 'Hiệu lực từ (YYYY-MM-DD)', 'text'],
    ['validTo', 'Hiệu lực đến (YYYY-MM-DD)', 'text'],
  ],
  DAY_TOUR: [
    ['durationHours', 'Thời lượng (giờ)', 'number'],
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

function khoiBanDau(sp: AdminProductDetail): Record<string, string> {
  const ten = KHOI_THEO_LOAI[sp.productType];
  const k = ten
    ? (sp as unknown as Record<string, Record<string, unknown> | undefined>)[ten]
    : undefined;
  if (!k) {
    return {};
  }
  return Object.fromEntries(Object.entries(k).map(([a, b]) => [a, String(b ?? '')]));
}

function khoiGui(productType: string, gt: Record<string, string>): Record<string, unknown> {
  const ten = KHOI_THEO_LOAI[productType];
  const truong = TRUONG_THEO_LOAI[productType] ?? [];
  if (!ten || truong.length === 0 || truong.some(([t]) => !gt[t])) {
    // Thiếu một trường thì KHÔNG gửi khối nào: PATCH bỏ qua khối vắng mặt, còn
    // gửi khối thiếu trường là chắc chắn 400.
    return {};
  }
  const noiDung = Object.fromEntries(
    truong.map(([t, , kieu]) => [t, kieu === 'number' ? Number(gt[t]) : gt[t]]),
  );
  return { [ten]: noiDung };
}
