'use client';

import Link from 'next/link';
import { use, useEffect, useState } from 'react';
import type { AdminBookingDetail } from '@travel/api-client';
import { formatMoney } from '@travel/ui';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { mauTrangThai, ngay, ngayGio, tenTacNhan, tenTrangThai } from '@/lib/don';

/**
 * Chi tiết đơn (docs/22 M7).
 *
 * Ba khối, và khối thứ ba là lý do màn hình này tồn tại: **toàn bộ** nhật ký
 * `booking_event`. Bảng đó chỉ ghi thêm (docs/11 mục 11.2), nên ở đây **không có
 * nút sửa và không có nút xoá** một dòng nhật ký — không phải vì khó làm, mà vì
 * có nút đó thì nhật ký hết giá trị.
 *
 * Màn hình này chỉ **đọc**. Đổi trạng thái đơn là một dòng riêng trong ma trận
 * quyền (docs/22 mục 2.1) và là việc của đợt sau.
 */
export default function ChiTietDon({ params }: { params: Promise<{ reference: string }> }) {
  const { reference } = use(params);

  const [don, setDon] = useState<AdminBookingDetail | null>(null);
  const [loi, setLoi] = useState('');

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ket_qua = await adminApi().chiTietDon({ reference });
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

      <section>
        <h2>Liên hệ và chuyến đi</h2>
        <table>
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
              <th>Số lượng</th>
              <th>Đơn giá</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            {don.breakdown.lines.map((d, i) => (
              <tr key={`${d.kind}-${i}`}>
                <td>{d.labelKey}</td>
                <td>{d.quantity ?? '—'}</td>
                <td>{d.unitAmount ? formatMoney(d.unitAmount, 'vi') : '—'}</td>
                <td>{formatMoney(d.amount, 'vi')}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={3}>
                Tổng
              </th>
              <td>
                <strong>{formatMoney(don.breakdown.total, 'vi')}</strong>
              </td>
            </tr>
            <tr>
              <th scope="row" colSpan={3}>
                Đặt cọc
              </th>
              <td>{formatMoney(don.breakdown.deposit, 'vi')}</td>
            </tr>
            <tr>
              <th scope="row" colSpan={3}>
                Còn lại
              </th>
              <td>{formatMoney(don.breakdown.balance, 'vi')}</td>
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

      <section>
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
