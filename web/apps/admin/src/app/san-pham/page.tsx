'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useState } from 'react';
import type { AdminProductPage, AdminProductSummary } from '@travel/api-client';
import { adminApi, laChuaDangNhap, loiTiengViet } from '@/lib/api';

const LOAI = [
  ['', 'Mọi loại'],
  ['GROUP_TOUR', 'Tour đoàn'],
  ['INDIVIDUAL_PACKAGE', 'Tour cá nhân'],
  ['PRIVATE_TOUR', 'Tour riêng'],
  ['CRUISE', 'Du thuyền'],
  ['COMBO', 'Combo'],
  ['DAY_TOUR', 'Tour trong ngày'],
] as const;

/**
 * Danh sách sản phẩm (docs/22 M2).
 *
 * **Trạng thái bộ lọc nằm trong URL**, không trong `useState` — F5 giữ nguyên bộ
 * lọc, và nhân viên gửi link màn hình đã lọc cho nhau được. Đó là thao tác hằng
 * ngày, không phải tính năng phụ (docs/22 mục 7).
 *
 * Phân trang **phía máy chủ**: danh sách chỉ lớn dần theo thời gian và không bao
 * giờ nhỏ lại.
 */
export default function DanhSachSanPham() {
  // useSearchParams đòi một ranh giới Suspense: nó chỉ có giá trị ở phía trình
  // duyệt, nên Next phải biết dựng gì trong lúc chưa có.
  return (
    <Suspense fallback={<main><div className="dang-tai" /></main>}>
      <NoiDung />
    </Suspense>
  );
}

function NoiDung() {
  const router = useRouter();
  const thamSo = useSearchParams();

  const productType = thamSo.get('productType') ?? '';
  const market = thamSo.get('market') ?? '';
  const gap = thamSo.get('gap') ?? '';
  const q = thamSo.get('q') ?? '';
  const page = Number(thamSo.get('page') ?? '0');

  // Kết quả mang theo BỘ LỌC đã sinh ra nó. Nhờ vậy "đang tải" là một phép so
  // sánh chứ không phải một state phải đặt lại ở đầu mỗi effect — và không có
  // khoảnh khắc nào bảng hiện dữ liệu của bộ lọc cũ.
  const khoa = JSON.stringify({ productType, market, gap, q, page });
  const [kq, setKq] = useState<{ khoa: string; trang: AdminProductPage } | null>(null);
  const [loi, setLoi] = useState('');

  const trang = kq?.khoa === khoa ? kq.trang : null;

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ket_qua = await adminApi().danhSachSanPhamQuanTri({
          productType: (productType || undefined) as never,
          market: (market || undefined) as never,
          gap: (gap || undefined) as never,
          q: q || undefined,
          page,
          size: 20,
        });
        if (conHieuLuc) {
          setKq({ khoa, trang: ket_qua });
        }
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(await loiTiengViet(ex));
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [khoa, productType, market, gap, q, page]);

  function doiLoc(ten: string, giaTri: string) {
    const moi = new URLSearchParams(thamSo.toString());
    if (giaTri) {
      moi.set(ten, giaTri);
    } else {
      moi.delete(ten);
    }
    // Đổi bộ lọc thì về trang đầu: giữ nguyên số trang là cách chắc chắn để ra
    // một trang rỗng và người dùng tưởng không có kết quả nào.
    moi.delete('page');
    router.push(`/san-pham?${moi.toString()}`);
  }

  return (
    <main>
      <h1>Sản phẩm</h1>
      <p className="phu">
        Mọi trạng thái, mọi ngôn ngữ, mọi thị trường — khác hẳn website khách, nơi
        sản phẩm chưa dịch hoặc chưa gán thị trường thì không tồn tại.
      </p>

      <div className="hang">
        <div>
          <label htmlFor="loai">Loại</label>
          <select
            id="loai"
            value={productType}
            onChange={(e) => doiLoc('productType', e.target.value)}
          >
            {LOAI.map(([ma, ten]) => (
              <option key={ma} value={ma}>
                {ten}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="tt">Đã gán thị trường</label>
          <select id="tt" value={market} onChange={(e) => doiLoc('market', e.target.value)}>
            <option value="">Mọi thị trường</option>
            <option value="DK">DK — Đan Mạch</option>
            <option value="VN">VN — Việt Nam</option>
          </select>
        </div>

        <div>
          <label htmlFor="gap">Tình trạng dịch</label>
          <select id="gap" value={gap} onChange={(e) => doiLoc('gap', e.target.value)}>
            <option value="">Tất cả</option>
            <option value="MISSING">Chưa dịch</option>
            <option value="OUTDATED">Bản dịch đã cũ</option>
          </select>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            const o = new FormData(e.currentTarget).get('q');
            doiLoc('q', String(o ?? '').trim());
          }}
        >
          <label htmlFor="q">Tìm tiêu đề</label>
          <div className="hang">
            {/* key + defaultValue: URL đổi thì ô nhập tự dựng lại theo. Đồng bộ
                bằng một effect thì phải đặt state ngay trong thân effect. */}
            <input
              key={q}
              id="q"
              name="q"
              defaultValue={q}
              placeholder="gõ không dấu cũng ra"
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
          Không có sản phẩm nào khớp bộ lọc. Bỏ bớt điều kiện, hoặc{' '}
          <button className="phu" onClick={() => router.push('/san-pham')}>
            xoá hết bộ lọc
          </button>
          .
        </p>
      )}

      {trang && trang.items.length > 0 && (
        <>
          <p className="phu" style={{ marginTop: '1.25rem' }}>
            {trang.totalItems} sản phẩm · trang {trang.page + 1}/{trang.totalPages}
          </p>

          <table>
            <thead>
              <tr>
                <th>Tiêu đề bản nguồn</th>
                <th>Loại</th>
                <th>Bản dịch</th>
                <th>Thị trường</th>
              </tr>
            </thead>
            <tbody>
              {trang.items.map((sp) => (
                <tr key={sp.id}>
                  <td>
                    <Link href={`/san-pham/${sp.id}`}>{sp.sourceTitle}</Link>
                  </td>
                  <td>{tenLoai(sp.productType)}</td>
                  <td>
                    <BanDich sp={sp} />
                  </td>
                  <td>
                    <ThiTruong sp={sp} />
                  </td>
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

function doiTrang(
  router: ReturnType<typeof useRouter>,
  thamSo: URLSearchParams,
  trang: number,
) {
  const moi = new URLSearchParams(thamSo.toString());
  moi.set('page', String(trang));
  router.push(`/san-pham?${moi.toString()}`);
}

function BanDich({ sp }: { sp: AdminProductSummary }) {
  return (
    <>
      {sp.translations.map((t) => (
        <span
          key={t.locale}
          className={`nhan ${t.status === 'PUBLISHED' && !t.outdated ? 'xanh' : t.outdated ? 'vang' : 'xam'}`}
          style={{ marginRight: '0.3rem' }}
          title={t.isSource ? 'Bản ngôn ngữ nguồn' : 'Bản dịch'}
        >
          {t.locale}
          {t.outdated ? ' đã cũ' : t.status === 'PUBLISHED' ? '' : ' nháp'}
        </span>
      ))}
    </>
  );
}

function ThiTruong({ sp }: { sp: AdminProductSummary }) {
  if (sp.markets.length === 0) {
    // Chưa gán thị trường nào nghĩa là sản phẩm KHÔNG tồn tại với khách, dù đã
    // dịch xong. Đây là câu trả lời cho "vì sao tour này chưa hiện trên web".
    return <span className="nhan vang">chưa gán</span>;
  }
  return (
    <>
      {sp.markets.map((m) => (
        <span
          key={m.market}
          className={`nhan ${m.published ? 'xanh' : 'xam'}`}
          style={{ marginRight: '0.3rem' }}
        >
          {m.market}
          {m.published ? '' : ' tắt'}
        </span>
      ))}
    </>
  );
}

function tenLoai(ma: string): string {
  return LOAI.find(([m]) => m === ma)?.[1] ?? ma;
}
