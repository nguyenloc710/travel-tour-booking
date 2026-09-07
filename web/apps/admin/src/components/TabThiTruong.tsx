'use client';

import { useState } from 'react';
import type { AdminProductDetail } from '@travel/api-client';
import { adminApi, laKhongDuQuyen, loiTiengViet } from '@/lib/api';

const THI_TRUONG = [
  ['DK', 'Đan Mạch — DKK'],
  ['VN', 'Việt Nam — VND'],
] as const;

/**
 * Gán sản phẩm vào thị trường và bật/tắt bán (docs/22 M5).
 *
 * **Chỉ vai trò `ADMIN`.** Đây là công tắc "bắt đầu bán được": dịch xong mà chưa
 * bật thì sản phẩm vẫn không tồn tại với khách. Nó nằm ở tay người chịu trách
 * nhiệm doanh thu, không ở tay người viết nội dung (docs/22 mục 2.1).
 */
export function TabThiTruong({
  sp,
  napLai,
}: {
  sp: AdminProductDetail;
  napLai: () => Promise<void>;
}) {
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState('');
  const [dangLuu, setDangLuu] = useState('');

  async function dat(market: string, published: boolean) {
    setLoi('');
    setXong('');
    setDangLuu(market);
    try {
      await adminApi().ganThiTruong({
        id: sp.id,
        market: market as never,
        adminMarketAssignment: { published },
      });
      setXong(
        published
          ? `Đã bật bán ở ${market}. Sản phẩm hiện trên website của thị trường này ngay.`
          : `Đã tắt bán ở ${market}. Sản phẩm biến khỏi website; đơn đã đặt không bị ảnh hưởng.`,
      );
      await napLai();
    } catch (ex) {
      setLoi(
        laKhongDuQuyen(ex)
          ? 'Chỉ vai trò ADMIN gán được sản phẩm vào thị trường.'
          : await loiTiengViet(ex),
      );
    } finally {
      setDangLuu('');
    }
  }

  return (
    <>
      <h2>Thị trường</h2>
      <p className="phu">
        Giá của mỗi thị trường là <strong>số người nhập</strong>, không phải kết quả
        tính từ thị trường kia. Tour bán cho khách Đan gồm vé bay quốc tế, bán cho
        khách Việt thì không — hai sản phẩm khác nhau.
      </p>

      {loi && <p className="loi">{loi}</p>}
      {xong && <p className="xong">{xong}</p>}

      <table>
        <thead>
          <tr>
            <th>Thị trường</th>
            <th>Trạng thái</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {THI_TRUONG.map(([ma, ten]) => {
            const gan = sp.markets.find((m) => m.market === ma);
            return (
              <tr key={ma}>
                <td>{ten}</td>
                <td>
                  {!gan && <span className="nhan vang">chưa gán</span>}
                  {gan?.published && <span className="nhan xanh">đang bán</span>}
                  {gan && !gan.published && <span className="nhan xam">đã gán, chưa bán</span>}
                </td>
                <td>
                  <div className="hang">
                    {!gan?.published && (
                      <button disabled={dangLuu === ma} onClick={() => dat(ma, true)}>
                        {gan ? 'Bật bán' : 'Gán và bật bán'}
                      </button>
                    )}
                    {gan?.published && (
                      <button className="phu" disabled={dangLuu === ma} onClick={() => dat(ma, false)}>
                        Tắt bán
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <p className="phu" style={{ marginTop: '1rem' }}>
        Bật bán không tự sinh giá. Sản phẩm chưa có ngày khởi hành nào kèm bảng giá
        thì website hiện &laquo;Liên hệ&raquo; thay vì hiện số — xem tab
        <strong> Ngày khởi hành và giá</strong>.
      </p>
    </>
  );
}
