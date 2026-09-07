'use client';

import { useState } from 'react';
import type { AdminDeparture, AdminDeparturePriceInput } from '@travel/api-client';
import { adminApi, laKhongDuQuyen, loiTiengViet } from '@/lib/api';

/**
 * Nhập bảng giá cho một ngày khởi hành (docs/22 M5).
 *
 * Ma trận **loại khách × kiểu phòng**. Lưu là thay **toàn bộ**, không sửa từng ô:
 * bảng giá thiếu một ô là một tổ hợp khách đặt được mà hệ thống không tính ra
 * giá, và không ai phát hiện cho tới khi đúng tổ hợp đó xuất hiện.
 *
 * **Không có ô tiền tệ.** Nó lấy từ cấu hình thị trường của chính ngày khởi hành
 * này — cho người nhập chọn tiền tệ là mở đường nhập giá DKK vào thị trường
 * `VN`, và không ràng buộc nào bắt được chuyện đó.
 */
export function BangGia({
  ngay,
  dong,
  napLai,
}: {
  ngay: AdminDeparture;
  dong: () => void;
  napLai: () => Promise<void>;
}) {
  // Mã loại khách nhập tay: `pax_type` là dữ liệu riêng từng thị trường và chưa
  // có endpoint liệt kê. Gõ sai thì máy chủ trả UNKNOWN_PAX_TYPE kèm đúng mã đã
  // gõ, nên sai vẫn sửa được ngay — chỉ là chưa tiện.
  const [dongGia, setDongGia] = useState<Dong[]>(
    ngay.prices.length > 0
      ? ngay.prices.map((g) => ({
          paxTypeCode: g.paxTypeCode,
          occupancy: g.occupancy,
          amount: g.amount.amount,
        }))
      : [
          { paxTypeCode: 'ADULT', occupancy: 'DOUBLE', amount: '' },
          { paxTypeCode: 'ADULT', occupancy: 'SINGLE', amount: '' },
        ],
  );
  const [loi, setLoi] = useState('');
  const [dangLuu, setDangLuu] = useState(false);

  function doi(i: number, truong: keyof Dong, gt: string) {
    setDongGia(dongGia.map((d, j) => (i === j ? { ...d, [truong]: gt } : d)));
  }

  async function luu() {
    setLoi('');
    setDangLuu(true);
    try {
      const gui: AdminDeparturePriceInput[] = dongGia
        .filter((d) => d.paxTypeCode.trim() && d.amount.trim())
        .map((d) => ({
          paxTypeCode: d.paxTypeCode.trim(),
          occupancy: d.occupancy as never,
          amount: d.amount.trim(),
        }));

      if (gui.length === 0) {
        setLoi('Cần ít nhất một dòng có mã loại khách và số tiền.');
        return;
      }

      await adminApi().luuGiaNgayKhoiHanh({ id: ngay.id, adminDeparturePriceInput: gui });
      await napLai();
      dong();
    } catch (ex) {
      setLoi(laKhongDuQuyen(ex) ? 'Chỉ vai trò ADMIN nhập được giá.' : await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  return (
    <div className="the" style={{ marginTop: '1rem' }}>
      <h2 style={{ marginTop: 0 }}>
        Giá ngày {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(ngay.departDate)}{' '}
        · thị trường {ngay.market}
      </h2>
      <p className="phu">
        Lưu là <strong>thay toàn bộ</strong> bảng giá của ngày này. Tiền tệ lấy từ
        thị trường {ngay.market}, không nhập ở đây.
      </p>

      <table>
        <thead>
          <tr>
            <th>Mã loại khách</th>
            <th>Kiểu phòng</th>
            <th>Số tiền</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {dongGia.map((d, i) => (
            <tr key={i}>
              <td>
                <input
                  value={d.paxTypeCode}
                  onChange={(e) => doi(i, 'paxTypeCode', e.target.value)}
                  placeholder="ADULT"
                  style={{ maxWidth: '11rem' }}
                />
              </td>
              <td>
                <select
                  value={d.occupancy}
                  onChange={(e) => doi(i, 'occupancy', e.target.value)}
                  style={{ maxWidth: '9rem' }}
                >
                  <option value="DOUBLE">Phòng đôi</option>
                  <option value="SINGLE">Phòng đơn</option>
                </select>
              </td>
              <td>
                <input
                  value={d.amount}
                  onChange={(e) => doi(i, 'amount', e.target.value)}
                  placeholder="24990.00"
                  inputMode="decimal"
                  style={{ maxWidth: '10rem' }}
                />
              </td>
              <td>
                <button
                  type="button"
                  className="phu"
                  onClick={() => setDongGia(dongGia.filter((_, j) => j !== i))}
                  aria-label="Bỏ dòng này"
                >
                  Bỏ
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <p className="phu" style={{ margin: '0.75rem 0 0' }}>
        Giá <strong>phòng đôi</strong> của loại khách <code>ADULT</code> là con số
        website hiện thành &laquo;giá từ&raquo;. Thiếu nó thì tour hiện
        &laquo;Liên hệ&raquo;.
      </p>

      {loi && <p className="loi">{loi}</p>}

      <div className="hang" style={{ marginTop: '1rem' }}>
        <button type="button" disabled={dangLuu} onClick={luu}>
          {dangLuu ? 'Đang lưu…' : 'Lưu bảng giá'}
        </button>
        <button
          type="button"
          className="phu"
          onClick={() =>
            setDongGia([...dongGia, { paxTypeCode: '', occupancy: 'DOUBLE', amount: '' }])
          }
        >
          Thêm dòng
        </button>
        <button type="button" className="phu" onClick={dong}>
          Đóng
        </button>
      </div>
    </div>
  );
}

type Dong = { paxTypeCode: string; occupancy: string; amount: string };
