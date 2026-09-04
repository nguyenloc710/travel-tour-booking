'use client';

import { useEffect, useState } from 'react';
import type { AdminStaffUser } from '@travel/api-client';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { VAI_TRO } from '@/lib/noiDung';

/**
 * Người dùng và vai trò (docs/22 M14) — **chỉ `ADMIN`**.
 *
 * Trước màn hình này, đổi vai trò một người là `INSERT` tay vào
 * `staff_user_role` trên cơ sở dữ liệu.
 *
 * **Không có nút Xoá, kể cả xoá mềm.** Người nghỉ việc vẫn đứng tên trong
 * `booking_event` và trong `last_modified_by` của mọi bản ghi họ từng sửa; xoá
 * họ đi là làm nhật ký kiểm toán trỏ vào hư không. Thay vào đó là công tắc
 * bật/tắt.
 */
export default function NguoiDung() {
  const [ds, setDs] = useState<AdminStaffUser[] | null>(null);
  const [loi, setLoi] = useState('');

  const [phienBan, setPhienBan] = useState(0);
  const napLai = async () => setPhienBan((v) => v + 1);

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const kq = await adminApi().danhSachNguoiDung();
        if (conHieuLuc) {
          setDs(kq);
          setLoi('');
        }
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(
            laKhongDuQuyen(ex)
              ? 'Chỉ quản trị viên vào được màn hình này. Vai trò của bạn không quản lý người dùng.'
              : await loiTiengViet(ex),
          );
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [phienBan]);

  return (
    <main>
      <div className="dau-trang">
        <div>
          <h1>Người dùng</h1>
          <p className="phu">
            Một người mang được nhiều vai trò. Tài khoản không dùng nữa thì <strong>tắt</strong>,
            không xoá — họ vẫn đứng tên trong nhật ký của những việc đã làm.
          </p>
        </div>
      </div>

      {loi && <p className="loi">{loi}</p>}

      {!ds && !loi && (
        <div>
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="dang-tai" />
          ))}
        </div>
      )}

      {ds?.map((u) => (
        <Nguoi key={u.id} u={u} napLai={napLai} />
      ))}
    </main>
  );
}

function Nguoi({ u, napLai }: { u: AdminStaffUser; napLai: () => Promise<void> }) {
  const hienCo = u.roles.map((r) => String(r));

  const [chon, setChon] = useState<string[]>(hienCo);
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  const daDoi = chon.length !== hienCo.length || chon.some((r) => !hienCo.includes(r));

  async function luuVaiTro() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().datVaiTro({
        id: u.id,
        adminRoleAssignment: { roles: chon as never[] },
      });
      await napLai();
      setXong(true);
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  async function doiBatTat() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().suaNguoiDung({
        id: u.id,
        adminStaffUserPatch: { isActive: !u.isActive },
      });
      await napLai();
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  return (
    <section className="the">
      <div className="dau-trang">
        <div>
          <h2>{u.displayName}</h2>
          <p className="phu">{u.email}</p>
        </div>
        <span
          className={`nhan ${u.isActive ? 'xanh' : 'xam'}`}
          style={{ marginLeft: 'auto', alignSelf: 'center' }}
        >
          {u.isActive ? 'Đang bật' : 'Đã tắt'}
        </span>
      </div>

      {loi && <p className="loi">{loi}</p>}
      {xong && !loi && <p className="xong">Đã lưu vai trò.</p>}

      <div className="loc">
        {VAI_TRO.map(([ma, ten, mo_ta]) => (
          <label key={ma} className="hang" style={{ gap: '0.5rem', alignItems: 'flex-start' }}>
            <input
              type="checkbox"
              checked={chon.includes(ma)}
              onChange={(e) =>
                setChon((cu) => (e.target.checked ? [...cu, ma] : cu.filter((x) => x !== ma)))
              }
            />
            <span>
              <strong>{ten}</strong> <span className="phu">({ma})</span>
              <br />
              <span className="phu">{mo_ta}</span>
            </span>
          </label>
        ))}

        <div className="hang">
          <button type="button" disabled={dangLuu || !daDoi} onClick={() => void luuVaiTro()}>
            {dangLuu ? 'Đang lưu…' : 'Lưu vai trò'}
          </button>
          <button type="button" className="phu" disabled={dangLuu} onClick={() => void doiBatTat()}>
            {u.isActive ? 'Tắt tài khoản' : 'Bật lại'}
          </button>
        </div>
      </div>
    </section>
  );
}
