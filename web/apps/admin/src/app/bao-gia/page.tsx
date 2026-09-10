'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useState } from 'react';
import type { AdminQuotePage } from '@travel/api-client';
import { formatMoney } from '@travel/ui';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { TRANG_THAI_CHON, conLai, mauTrangThai, ngay, ngayGio, tenTrangThai } from '@/lib/baoGia';

/**
 * Danh sách báo giá (docs/22 M8).
 *
 * **Mặc định là `DRAFT`, không phải tất cả** — cùng lý do với `NEEDS_ACTION` ở
 * danh sách đơn, và mặc định đó nằm ở *backend*: một danh sách vận hành mà mặc
 * định do frontend quyết thì mỗi chỗ gọi lại ra một mặc định khác.
 *
 * **Sắp cũ nhất trước**, ngược với danh sách đơn. Đây là hàng đợi việc, mà việc
 * chờ lâu nhất thì gấp nhất — một yêu cầu báo giá để quên ba ngày là một đơn đã
 * mất.
 */
export default function DanhSachBaoGia() {
  return (
    <Suspense fallback={<main><div className="dang-tai" /></main>}>
      <NoiDung />
    </Suspense>
  );
}

function NoiDung() {
  const router = useRouter();
  const thamSo = useSearchParams();

  const status = thamSo.get('status') ?? '';
  const market = thamSo.get('market') ?? '';
  const q = thamSo.get('q') ?? '';
  const page = Number(thamSo.get('page') ?? '0');

  const khoa = JSON.stringify({ status, market, q, page });
  const [kq, setKq] = useState<{ khoa: string; trang: AdminQuotePage } | null>(null);
  const [loi, setLoi] = useState('');

  const trang = kq?.khoa === khoa ? kq.trang : null;

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ket_qua = await adminApi().listQuotes({
          status: (status || undefined) as never,
          market: (market || undefined) as never,
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
              ? 'Vai trò của bạn không xem được báo giá. Chỉ tư vấn viên và quản trị viên vào được màn hình này.'
              : await loiTiengViet(ex),
          );
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [khoa, status, market, q, page]);

  function doiLoc(ten: string, giaTri: string) {
    const moi = new URLSearchParams(thamSo.toString());
    if (giaTri) {
      moi.set(ten, giaTri);
    } else {
      moi.delete(ten);
    }
    // Đổi bộ lọc thì về trang đầu — giữ số trang cũ là cách chắc chắn để ra một
    // trang rỗng và người dùng tưởng không có báo giá nào.
    moi.delete('page');
    router.push(`/bao-gia?${moi.toString()}`);
  }

  return (
    <main>
      <div className="dau-trang">
        <div>
          <h1>Báo giá</h1>
          <p className="phu">
            Chỉ tour riêng. Mặc định hiện những yêu cầu <strong>chưa ai dựng giá</strong>,
            cũ nhất trước — chờ lâu nhất thì gấp nhất.
          </p>
        </div>
      </div>

      <div className="loc hang">
        <div>
          <label htmlFor="status">Trạng thái</label>
          <select id="status" value={status} onChange={(e) => doiLoc('status', e.target.value)}>
            <option value="">Chờ dựng giá</option>
            <option value="ALL">Mọi trạng thái</option>
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

        <form
          onSubmit={(e) => {
            e.preventDefault();
            const o = new FormData(e.currentTarget).get('q');
            doiLoc('q', String(o ?? '').trim());
          }}
        >
          <label htmlFor="q">Mã, tên hoặc email khách</label>
          <div className="hang">
            <input
              key={q}
              id="q"
              name="q"
              defaultValue={q}
              placeholder="Q-DK-2026-… hoặc tên khách"
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
          {status ? (
            <>
              Không có báo giá nào khớp bộ lọc.{' '}
              <button className="phu" onClick={() => router.push('/bao-gia')}>
                xoá hết bộ lọc
              </button>
              .
            </>
          ) : (
            <>
              Không còn yêu cầu nào chờ dựng giá. Xem{' '}
              <button className="phu" onClick={() => doiLoc('status', 'ALL')}>
                mọi trạng thái
              </button>{' '}
              nếu đang tìm một báo giá đã gửi.
            </>
          )}
        </p>
      )}

      {trang && trang.items.length > 0 && (
        <>
          <p className="phu" style={{ marginTop: '1.25rem' }}>
            {trang.totalItems} báo giá · trang {trang.page + 1}/{trang.totalPages}
          </p>

          <table>
            <thead>
              <tr>
                <th>Mã</th>
                <th>Trạng thái</th>
                <th>Tour</th>
                <th>Khách</th>
                <th>Số người</th>
                <th>Ngày muốn đi</th>
                <th>Tổng</th>
                <th>Hạn</th>
                <th>Gửi lúc</th>
              </tr>
            </thead>
            <tbody>
              {trang.items.map((b) => (
                <tr key={b.id}>
                  <td>
                    <Link href={`/bao-gia/${b.reference}`}>{b.reference}</Link>
                  </td>
                  <td>
                    <span className={`nhan ${mauTrangThai(b.status)}`}>
                      {tenTrangThai(b.status)}
                    </span>
                  </td>
                  <td>{b.productTitle}</td>
                  <td>
                    {b.contactName}
                    <br />
                    {/* Thị trường và ngôn ngữ cạnh nhau: tư vấn viên gọi lại cần
                        biết nói tiếng gì, và ngôn ngữ KHÔNG suy ra được từ thị
                        trường (docs/02). */}
                    <span className="phu">
                      {b.market} · {b.locale}
                    </span>
                  </td>
                  <td>{b.partySize}</td>
                  <td>{ngay(b.requestedDate)}</td>
                  {/* Chưa dựng giá thì KHÔNG hiện số 0 — một báo giá 0 đồng đọc
                      lên như một chuyến đi miễn phí. */}
                  <td>{b.total ? formatMoney(b.total, 'vi') : '—'}</td>
                  <td>
                    <Han validUntil={b.validUntil} />
                  </td>
                  <td>{ngayGio(b.createdAt)}</td>
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
 * Hạn hiệu lực, kèm số ngày còn lại.
 *
 * Ngày không thì đọc được nhưng không **dùng** được: nhân viên phải tự trừ ngày
 * để biết cái nào sắp hết. Con số "còn 2 ngày" là thứ quyết định gọi ai trước.
 */
function Han({ validUntil }: { validUntil: Date | undefined }) {
  const con = conLai(validUntil);
  if (con === null) {
    return <>—</>;
  }
  return (
    <>
      {ngay(validUntil)}
      <br />
      <span className="phu">{con < 0 ? 'đã quá hạn' : `còn ${con} ngày`}</span>
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
  router.push(`/bao-gia?${moi.toString()}`);
}
