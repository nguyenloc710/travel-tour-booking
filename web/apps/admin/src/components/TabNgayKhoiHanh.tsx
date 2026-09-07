'use client';

import { useCallback, useEffect, useState } from 'react';
import type { AdminDeparture, AdminProductDetail } from '@travel/api-client';
import { adminApi, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { BangGia } from '@/components/BangGia';

/**
 * Ngày khởi hành và bảng giá (docs/22 M4 và M5).
 *
 * Một sản phẩm bán ở cả hai thị trường có **hai bộ** ngày khởi hành, không phải
 * một bộ dùng chung (ADR-006). Nhập tay hai lần là chắc chắn lệch, nên có nút
 * nhân bản lịch — nhưng nó **chỉ chép lịch, không chép giá**.
 */
export function TabNgayKhoiHanh({ sp }: { sp: AdminProductDetail }) {
  const [ds, setDs] = useState<AdminDeparture[] | null>(null);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState('');
  const [dangSuaGia, setDangSuaGia] = useState<string | null>(null);

  const nap = useCallback(async () => {
    try {
      setDs(await adminApi().danhSachNgayKhoiHanhQuanTri({ id: sp.id }));
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    }
  }, [sp.id]);

  useEffect(() => {
    void (async () => {
      await nap();
    })();
  }, [nap]);

  async function nhanBan(tu: string, sang: string) {
    setLoi('');
    setXong('');
    try {
      const kq = await adminApi().nhanBanLichKhoiHanh({
        id: sp.id,
        adminDepartureCopy: { fromMarket: tu as never, toMarket: sang as never },
      });
      setXong(
        `Đã tạo ${kq.created} ngày ở ${sang}, bỏ qua ${kq.skipped} ngày đã có. ` +
          'Giá KHÔNG được chép sang — mỗi thị trường một bảng giá do người nhập.',
      );
      await nap();
    } catch (ex) {
      setLoi(laKhongDuQuyen(ex) ? 'Chỉ vai trò ADMIN làm được việc này.' : await loiTiengViet(ex));
    }
  }

  return (
    <>
      <h2>Ngày khởi hành</h2>

      <TaoNgay productId={sp.id} napLai={nap} setLoi={setLoi} setXong={setXong} />

      {loi && <p className="loi">{loi}</p>}
      {xong && <p className="xong">{xong}</p>}

      {ds === null && !loi && <div className="dang-tai" />}

      {ds?.length === 0 && (
        <p className="trong">
          Chưa có ngày khởi hành nào. Chưa có ngày thì khách không đặt được, và
          website hiện &laquo;Liên hệ&raquo; thay vì hiện giá.
        </p>
      )}

      {ds && ds.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Thị trường</th>
              <th>Ngày đi</th>
              <th>Ngày về</th>
              <th>Chỗ</th>
              <th>Trạng thái</th>
              <th>Giá phòng đôi</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {ds.map((d) => (
              <tr key={d.id}>
                <td>{d.market}</td>
                <td>{ngay(d.departDate)}</td>
                <td>{ngay(d.returnDate)}</td>
                <td>
                  {d.seatsBooked}/{d.capacity}
                </td>
                <td>
                  <span className="nhan xam">{d.baseStatus}</span>
                </td>
                <td>
                  {d.prices.length === 0 ? (
                    <span className="nhan vang">chưa có giá</span>
                  ) : (
                    d.prices
                      .filter((g) => g.occupancy === 'DOUBLE')
                      .map((g) => `${g.paxTypeCode} ${g.amount.amount} ${g.amount.currency}`)
                      .join(' · ')
                  )}
                </td>
                <td>
                  <button
                    className="phu"
                    onClick={() => setDangSuaGia(dangSuaGia === d.id ? null : d.id)}
                  >
                    {d.prices.length === 0 ? 'Nhập giá' : 'Sửa giá'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {dangSuaGia && ds && (
        <BangGia
          key={dangSuaGia}
          ngay={ds.find((d) => d.id === dangSuaGia)!}
          dong={() => setDangSuaGia(null)}
          napLai={nap}
        />
      )}

      <h2>Nhân bản lịch sang thị trường kia</h2>
      <p className="phu">
        Chép ngày đi, số ngày, sức chứa và hạng cabin. <strong>Không chép giá</strong> —
        giá của mỗi thị trường là số người nhập, không phải kết quả tính từ thị
        trường kia. Ngày đã có ở thị trường đích thì bỏ qua, không ghi đè.
      </p>
      <div className="hang">
        <button className="phu" onClick={() => nhanBan('DK', 'VN')}>
          DK → VN
        </button>
        <button className="phu" onClick={() => nhanBan('VN', 'DK')}>
          VN → DK
        </button>
      </div>
    </>
  );
}

function TaoNgay({
  productId,
  napLai,
  setLoi,
  setXong,
}: {
  productId: string;
  napLai: () => Promise<void>;
  setLoi: (s: string) => void;
  setXong: (s: string) => void;
}) {
  const [market, setMarket] = useState('DK');
  const [departDate, setDepartDate] = useState('');
  const [days, setDays] = useState('14');
  const [capacity, setCapacity] = useState('20');
  const [dangGui, setDangGui] = useState(false);

  async function tao(e: React.FormEvent) {
    e.preventDefault();
    setLoi('');
    setXong('');
    setDangGui(true);
    try {
      await adminApi().taoNgayKhoiHanh({
        id: productId,
        adminDepartureCreate: {
          market: market as never,
          departDate: new Date(departDate),
          days: Number(days),
          capacity: Number(capacity),
        },
      });
      setXong('Đã tạo. Ngày mới chưa có giá — nhập giá trước khi bán.');
      setDepartDate('');
      await napLai();
    } catch (ex) {
      setLoi(laKhongDuQuyen(ex) ? 'Chỉ vai trò ADMIN tạo được ngày khởi hành.' : await loiTiengViet(ex));
    } finally {
      setDangGui(false);
    }
  }

  return (
    <form onSubmit={tao} className="hang" style={{ marginBottom: '1rem' }}>
      <div>
        <label htmlFor="tt">Thị trường</label>
        <select id="tt" value={market} onChange={(e) => setMarket(e.target.value)}>
          <option value="DK">DK</option>
          <option value="VN">VN</option>
        </select>
      </div>
      <div>
        <label htmlFor="di">Ngày đi</label>
        <input
          id="di"
          type="date"
          required
          value={departDate}
          onChange={(e) => setDepartDate(e.target.value)}
          style={{ width: '11rem' }}
        />
      </div>
      <div>
        <label htmlFor="so-ngay-kh">Số ngày</label>
        <input
          id="so-ngay-kh"
          type="number"
          min={1}
          max={60}
          required
          value={days}
          onChange={(e) => setDays(e.target.value)}
          style={{ width: '6rem' }}
        />
      </div>
      <div>
        <label htmlFor="cho">Sức chứa</label>
        <input
          id="cho"
          type="number"
          min={1}
          required
          value={capacity}
          onChange={(e) => setCapacity(e.target.value)}
          style={{ width: '6rem' }}
        />
      </div>
      <button type="submit" disabled={dangGui}>
        Thêm ngày
      </button>
      {/* Ngày về không có ô nhập: nó bằng ngày đi + số ngày − 1, và máy chủ tự
          suy. Nhận cả hai đầu là mời hai giá trị mâu thuẫn nhau. */}
    </form>
  );
}

function ngay(d: Date): string {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short' }).format(d);
}
