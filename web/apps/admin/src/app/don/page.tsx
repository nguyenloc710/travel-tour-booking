'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useState } from 'react';
import type { AdminBookingPage, AdminBookingSummary } from '@travel/api-client';
import { formatMoney } from '@travel/ui';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { TRANG_THAI_CHON, mauTrangThai, ngay, ngayGio, tenTrangThai } from '@/lib/don';

/**
 * Danh sách đơn đặt (docs/22 M6).
 *
 * **Mặc định là "cần xử lý", không phải "tất cả"** — và mặc định đó nằm ở
 * *backend*, không ở đây: một danh sách vận hành mà mặc định của nó do frontend
 * quyết thì mỗi chỗ gọi lại ra một mặc định khác.
 *
 * Trạng thái bộ lọc nằm trong URL, cùng lý do với màn hình sản phẩm: nhân viên
 * gửi link màn hình đã lọc cho nhau, và F5 phải giữ nguyên.
 */
export default function DanhSachDon() {
  return (
    <Suspense fallback={<main><div className="dang-tai" /></main>}>
      <NoiDung />
    </Suspense>
  );
}

function NoiDung() {
  const router = useRouter();
  const thamSo = useSearchParams();

  const scope = thamSo.get('scope') ?? '';
  const status = thamSo.get('status') ?? '';
  const market = thamSo.get('market') ?? '';
  const from = thamSo.get('from') ?? '';
  const to = thamSo.get('to') ?? '';
  const q = thamSo.get('q') ?? '';
  const page = Number(thamSo.get('page') ?? '0');

  const khoa = JSON.stringify({ scope, status, market, from, to, q, page });
  const [kq, setKq] = useState<{ khoa: string; trang: AdminBookingPage } | null>(null);
  const [loi, setLoi] = useState('');

  const trang = kq?.khoa === khoa ? kq.trang : null;

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ket_qua = await adminApi().listBookings({
          scope: (scope || undefined) as never,
          status: (status || undefined) as never,
          market: (market || undefined) as never,
          // Client sinh từ spec nhận `Date` cho trường `format: date`. Chuỗi
          // `yyyy-mm-dd` từ URL dựng bằng `new Date(...)` được đọc là UTC, đúng
          // với hợp đồng: khoảng ngày của endpoint này tính theo UTC.
          from: from ? new Date(from) : undefined,
          to: to ? new Date(to) : undefined,
          q: q || undefined,
          page,
          size: 20,
        });
        if (conHieuLuc) {
          setKq({ khoa, trang: ket_qua });
          setLoi('');
        }
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(
            laKhongDuQuyen(ex)
              ? 'Vai trò của bạn không xem được đơn đặt. Chỉ tư vấn viên và quản trị viên vào được màn hình này.'
              : await loiTiengViet(ex),
          );
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [khoa, scope, status, market, from, to, q, page]);

  function doiLoc(ten: string, giaTri: string) {
    const moi = new URLSearchParams(thamSo.toString());
    if (giaTri) {
      moi.set(ten, giaTri);
    } else {
      moi.delete(ten);
    }
    // Đổi bộ lọc thì về trang đầu — giữ số trang cũ là cách chắc chắn để ra một
    // trang rỗng và người dùng tưởng không có đơn nào.
    moi.delete('page');
    router.push(`/don?${moi.toString()}`);
  }

  return (
    <main>
      <div className="dau-trang">
        <div>
          <h1>Đơn đặt</h1>
          <p className="phu">
            Mặc định chỉ hiện đơn <strong>cần xử lý</strong> — nháp, chờ thanh toán, chờ
            xác nhận. Đơn đã xong nằm ở &ldquo;Mọi trạng thái&rdquo;.
          </p>
        </div>
      </div>

      <div className="loc hang">
        <div>
          <label htmlFor="scope">Phạm vi</label>
          <select
            id="scope"
            value={scope}
            disabled={status !== ''}
            title={status ? 'Đang lọc theo một trạng thái cụ thể' : undefined}
            onChange={(e) => doiLoc('scope', e.target.value)}
          >
            <option value="">Cần xử lý</option>
            <option value="ALL">Mọi trạng thái</option>
          </select>
        </div>

        <div>
          <label htmlFor="status">Trạng thái</label>
          <select id="status" value={status} onChange={(e) => doiLoc('status', e.target.value)}>
            <option value="">— theo phạm vi —</option>
            {TRANG_THAI_CHON.map(([ma, ten]) => (
              <option key={ma} value={ma}>
                {ten}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="tt">Thị trường</label>
          <select id="tt" value={market} onChange={(e) => doiLoc('market', e.target.value)}>
            <option value="">Cả hai</option>
            <option value="DK">DK — Đan Mạch</option>
            <option value="VN">VN — Việt Nam</option>
          </select>
        </div>

        <div>
          <label htmlFor="from">Đặt từ ngày</label>
          <input
            id="from"
            type="date"
            value={from}
            onChange={(e) => doiLoc('from', e.target.value)}
          />
        </div>

        <div>
          <label htmlFor="to">Đến ngày</label>
          <input id="to" type="date" value={to} onChange={(e) => doiLoc('to', e.target.value)} />
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            const o = new FormData(e.currentTarget).get('q');
            doiLoc('q', String(o ?? '').trim());
          }}
        >
          <label htmlFor="q">Mã đơn hoặc email</label>
          <div className="hang">
            <input
              key={q}
              id="q"
              name="q"
              defaultValue={q}
              placeholder="DK-2026-… hoặc email"
              style={{ width: '14rem' }}
            />
            <button type="submit" className="phu">
              Tìm
            </button>
          </div>
        </form>
      </div>

      {loi && <p className="loi">{loi}</p>}

      {trang === null && !loi && (
        <div style={{ marginTop: '1.5rem' }}>
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="dang-tai" />
          ))}
        </div>
      )}

      {trang?.items.length === 0 && (
        <p className="trong">
          {scope === 'ALL' || status ? (
            <>
              Không có đơn nào khớp bộ lọc.{' '}
              <button className="phu" onClick={() => router.push('/don')}>
                xoá hết bộ lọc
              </button>
              .
            </>
          ) : (
            <>
              Không còn đơn nào chờ xử lý. Xem{' '}
              <button className="phu" onClick={() => doiLoc('scope', 'ALL')}>
                mọi trạng thái
              </button>{' '}
              nếu đang tìm một đơn đã xong.
            </>
          )}
        </p>
      )}

      {trang && trang.items.length > 0 && (
        <>
          <p className="phu" style={{ marginTop: '1.25rem' }}>
            {trang.totalItems} đơn · trang {trang.page + 1}/{trang.totalPages}
          </p>

          <table>
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Trạng thái</th>
                <th>Tour</th>
                <th>Khởi hành</th>
                <th>Khách</th>
                <th>Tổng</th>
                <th>Liên hệ</th>
                <th>Đặt lúc</th>
              </tr>
            </thead>
            <tbody>
              {trang.items.map((d) => (
                <tr key={d.id}>
                  <td>
                    <Link href={`/don/${d.reference}`}>{d.reference}</Link>
                  </td>
                  <td>
                    <span className={`nhan ${mauTrangThai(d.status)}`}>
                      {tenTrangThai(d.status)}
                    </span>
                  </td>
                  <td>{d.productTitle}</td>
                  <td>{ngay(d.departDate)}</td>
                  <td>{d.paxCount}</td>
                  <td>{formatMoney(d.total, 'vi')}</td>
                  <td>
                    <Lienhe d={d} />
                  </td>
                  <td>{ngayGio(d.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="hang" style={{ marginTop: '1rem' }}>
            <button
              className="phu"
              disabled={trang.page === 0}
              onClick={() => doiTrang(router, thamSo, trang.page - 1)}
            >
              ← Trang trước
            </button>
            <button
              className="phu"
              disabled={trang.page + 1 >= trang.totalPages}
              onClick={() => doiTrang(router, thamSo, trang.page + 1)}
            >
              Trang sau →
            </button>
          </div>
        </>
      )}
    </main>
  );
}

/**
 * Email kèm **thị trường và ngôn ngữ**.
 *
 * Hai chữ đó cạnh nhau là có chủ ý: nhân viên gọi lại cần biết nói tiếng gì, và
 * ngôn ngữ **không** suy ra được từ thị trường — khách Việt sống ở Đan Mạch mua
 * ở `DK` mà đọc `vi` (docs/02).
 */
function Lienhe({ d }: { d: AdminBookingSummary }) {
  return (
    <>
      {d.contactEmail}
      <br />
      <span className="phu">
        {d.market} · {d.locale}
      </span>
    </>
  );
}

function doiTrang(
  router: ReturnType<typeof useRouter>,
  thamSo: URLSearchParams,
  trang: number,
) {
  const moi = new URLSearchParams(thamSo.toString());
  moi.set('page', String(trang));
  router.push(`/don?${moi.toString()}`);
}
