'use client';

import Link from 'next/link';
import { use, useEffect, useState } from 'react';
import type { AdminBookingDetail } from '@travel/api-client';
import { formatMoney } from '@travel/ui';
import { adminApi, errorInfo, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import {
  hauQua,
  mauTrangThai,
  ngay,
  ngayGio,
  tenDongGia,
  tenTacNhan,
  tenTrangThai,
  thaoTacChoPhep,
} from '@/lib/don';

/**
 * Chi tiết đơn (docs/22 M7).
 *
 * Ba khối, và khối thứ ba là lý do màn hình này tồn tại: **toàn bộ** nhật ký
 * `booking_event`. Bảng đó chỉ ghi thêm (docs/11 mục 11.2), nên ở đây **không có
 * nút sửa và không có nút xoá** một dòng nhật ký — không phải vì khó làm, mà vì
 * có nút đó thì nhật ký hết giá trị.
 *
 * Khối "Thao tác" là đường GHI: nhân viên đổi trạng thái đơn, mỗi lần đổi ghi
 * một dòng nhật ký. Quyền của nó là một dòng RIÊNG trong ma trận docs/22 mục
 * 2.1, tách khỏi quyền xem — ở v1 hai dòng trùng vai trò, nhưng vẫn kiểm riêng.
 */
export default function ChiTietDon({ params }: { params: Promise<{ reference: string }> }) {
  const { reference } = use(params);

  const [don, setDon] = useState<AdminBookingDetail | null>(null);
  const [loi, setLoi] = useState('');

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ket_qua = await adminApi().getAdminBooking({ reference });
        if (conHieuLuc) {
          setDon(ket_qua);
        }
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(
            laKhongDuQuyen(ex)
              ? 'Vai trò của bạn không xem được đơn đặt.'
              : await loiTiengViet(ex),
          );
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [reference]);

  if (loi) {
    return (
      <main>
        <p className="loi">{loi}</p>
        <p>
          <Link href="/don">← Về danh sách đơn</Link>
        </p>
      </main>
    );
  }

  if (!don) {
    return (
      <main>
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="dang-tai" />
        ))}
      </main>
    );
  }

  return (
    <main>
      <p className="phu">
        <Link href="/don">← Đơn đặt</Link>
      </p>

      <div className="dau-trang">
        <div>
          <h1>{don.reference}</h1>
          <p className="phu">
            {don.productTitle}
            {' · '}
            {don.market} · {don.locale}
            {' · đặt lúc '}
            {ngayGio(don.createdAt)}
          </p>
        </div>
        <span
          className={`nhan ${mauTrangThai(don.status)}`}
          style={{ marginLeft: 'auto', alignSelf: 'center' }}
        >
          {tenTrangThai(don.status)}
        </span>
      </div>

      <ThaoTac don={don} onXong={setDon} />

      <section>
        <h2>Liên hệ và chuyến đi</h2>
        <table className="bang-doi">
          <tbody>
            <tr>
              <th scope="row">Email</th>
              <td>{don.contactEmail}</td>
            </tr>
            <tr>
              <th scope="row">Điện thoại</th>
              <td>{don.contactPhone}</td>
            </tr>
            <tr>
              <th scope="row">Ngày khởi hành</th>
              <td>{ngay(don.departDate)}</td>
            </tr>
            <tr>
              <th scope="row">Ngôn ngữ khách đọc</th>
              {/* Đứng riêng một dòng vì đây là thứ hay bị suy nhầm từ thị
                  trường. Nhân viên gọi lại cần biết nói tiếng gì. */}
              <td>{don.locale}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2>Phân rã giá</h2>
        <p className="phu">
          Đây là giá <strong>đã chụp lại lúc đặt</strong>. Đổi bảng giá hôm nay không làm
          đổi đơn cũ.
        </p>
        <table>
          <thead>
            <tr>
              <th>Dòng</th>
              <th className="so">Số lượng</th>
              <th className="so">Đơn giá</th>
              <th className="so">Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            {don.breakdown.lines.map((d, i) => (
              <tr key={`${d.kind}-${i}`}>
                <td>{tenDongGia(d.kind, d.labelKey)}</td>
                <td className="so">{d.quantity ?? '—'}</td>
                <td className="so">{d.unitAmount ? formatMoney(d.unitAmount, 'vi') : '—'}</td>
                <td className="so">{formatMoney(d.amount, 'vi')}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={3}>
                Tổng
              </th>
              <td className="so">
                <strong>{formatMoney(don.breakdown.total, 'vi')}</strong>
              </td>
            </tr>
            <tr>
              <th scope="row" colSpan={3}>
                Đặt cọc
              </th>
              <td className="so">{formatMoney(don.breakdown.deposit, 'vi')}</td>
            </tr>
            <tr>
              <th scope="row" colSpan={3}>
                Còn lại
              </th>
              <td className="so">{formatMoney(don.breakdown.balance, 'vi')}</td>
            </tr>
          </tfoot>
        </table>
      </section>

      <section>
        <h2>Hành khách ({don.passengers.length})</h2>
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Họ tên</th>
              <th>Loại khách</th>
              <th>Ngày sinh</th>
              <th>Hộ chiếu</th>
              <th>Quốc tịch</th>
            </tr>
          </thead>
          <tbody>
            {don.passengers.map((k) => (
              <tr key={k.seq}>
                <td>{k.seq}</td>
                <td>{k.fullName}</td>
                <td>{k.paxTypeCode}</td>
                <td>{ngay(k.dateOfBirth)}</td>
                <td>
                  {k.passportNo ? (
                    <>
                      {k.passportNo}
                      <br />
                      <span className="phu">hết hạn {ngay(k.passportExpiry)}</span>
                    </>
                  ) : (
                    <span className="phu">chưa có</span>
                  )}
                </td>
                <td>{k.nationality ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section id="nhat-ky">
        <h2>Nhật ký ({don.events.length})</h2>
        <p className="phu">
          Toàn bộ lịch sử của đơn, cũ nhất trước. Bảng này <strong>chỉ ghi thêm</strong>:
          không sửa được, không xoá được dòng nào.
        </p>
        <table>
          <thead>
            <tr>
              <th>Lúc</th>
              <th>Chuyển</th>
              <th>Ai</th>
              <th>Ghi chú</th>
            </tr>
          </thead>
          <tbody>
            {don.events.map((e) => (
              <tr key={e.id}>
                <td>{ngayGio(e.createdAt)}</td>
                <td>
                  {/* Dòng đầu không đến từ trạng thái nào — đơn vừa sinh ra. */}
                  {e.fromStatus ? `${tenTrangThai(e.fromStatus)} → ` : ''}
                  <span className={`nhan ${mauTrangThai(e.toStatus)}`}>
                    {tenTrangThai(e.toStatus)}
                  </span>
                </td>
                <td>
                  {tenTacNhan(e.actorType)}
                  {e.actorName ? (
                    <>
                      <br />
                      <span className="phu">{e.actorName}</span>
                    </>
                  ) : null}
                </td>
                <td>{e.note ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  );
}

/**
 * Khối thao tác — đường **ghi** của M7.
 *
 * Nút chỉ hiện với bước chuyển máy trạng thái cho phép, và mỗi bước phải qua một
 * ô xác nhận **nói rõ hậu quả** (`docs/22` mục 7). "Huỷ đơn này và trả 2 chỗ về
 * kho, không đảo ngược được" khác hẳn "Bạn có chắc không?" — câu thứ hai không
 * cho người bấm thêm thông tin nào để quyết.
 *
 * Trạng thái đã chốt thì khối này biến mất hẳn thay vì hiện nút xám: một hàng
 * nút không bấm được chỉ làm người dùng thử rồi thắc mắc.
 */
function ThaoTac({
  don,
  onXong,
}: {
  don: AdminBookingDetail;
  onXong: (d: AdminBookingDetail) => void;
}) {
  const [chon, setChon] = useState<string | null>(null);
  const [ghiChu, setGhiChu] = useState('');
  const [dangGui, setDangGui] = useState(false);
  const [loi, setLoi] = useState('');

  const thaoTac = thaoTacChoPhep(don.status);
  if (thaoTac.length === 0) {
    return null;
  }

  async function gui(sang: string) {
    setDangGui(true);
    setLoi('');
    try {
      const moi = await adminApi().changeBookingStatus({
        reference: don.reference,
        adminBookingStatusChange: {
          toStatus: sang as never,
          note: ghiChu.trim() || undefined,
        },
      });
      // Máy chủ trả về cả đơn kèm nhật ký đã có dòng mới, nên không cần gọi
      // lại lần hai để làm mới màn hình.
      onXong(moi);
      setChon(null);
      setGhiChu('');
    } catch (ex) {
      setLoi(await loiDoiTrangThai(ex));
    } finally {
      setDangGui(false);
    }
  }

  return (
    <section>
      <h2>Thao tác</h2>

      {loi && <p className="loi">{loi}</p>}

      {chon === null && (
        <div className="hang">
          {thaoTac.map((t) => (
            <button
              key={t.sang}
              type="button"
              className={t.nang ? undefined : 'phu'}
              onClick={() => {
                setLoi('');
                setChon(t.sang);
              }}
            >
              {t.nhan}
            </button>
          ))}
        </div>
      )}

      {chon !== null && (
        <div className="loc">
          <p>
            <strong>{hauQua(chon, don.passengers.length)}</strong>
          </p>
          <div>
            <label htmlFor="ghi-chu">Ghi chú — vào thẳng nhật ký của đơn</label>
            <input
              id="ghi-chu"
              value={ghiChu}
              maxLength={500}
              placeholder="vì sao đổi, ai yêu cầu"
              onChange={(e) => setGhiChu(e.target.value)}
              style={{ width: '100%', maxWidth: '32rem' }}
            />
          </div>
          <div className="hang" style={{ marginTop: '0.75rem' }}>
            <button type="button" disabled={dangGui} onClick={() => void gui(chon)}>
              {dangGui ? 'Đang lưu…' : 'Xác nhận'}
            </button>
            <button
              type="button"
              className="phu"
              disabled={dangGui}
              onClick={() => {
                setChon(null);
                setGhiChu('');
              }}
            >
              Thôi
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

/**
 * Một mã lỗi riêng của màn hình này, ngoài bảng dùng chung ở `lib/api.ts`.
 *
 * `BOOKING_TRANSITION_NOT_ALLOWED` mang `from` và `to`, và câu dựng từ hai tham
 * số đó nói đúng chuyện đã xảy ra: gần như luôn là **người khác vừa đổi trạng
 * thái đơn này** trong lúc màn hình đang mở.
 */
async function loiDoiTrangThai(ex: unknown): Promise<string> {
  const than = await errorInfo(ex);
  if (than?.code === 'BOOKING_TRANSITION_NOT_ALLOWED') {
    return `Đơn đang ở trạng thái "${tenTrangThai(String(than.params.from))}" nên không chuyển sang "${tenTrangThai(String(than.params.to))}" được. Nhiều khả năng người khác vừa đổi — tải lại trang để xem trạng thái mới nhất.`;
  }
  return loiTiengViet(ex);
}
