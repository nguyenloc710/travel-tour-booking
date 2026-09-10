'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useState } from 'react';
import type {
  AdminDestination,
  AdminLecturePage,
  AdminPostPage,
} from '@travel/api-client';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import {
  LOAI_NOI_DUNG,
  laLoaiNoiDung,
  mauTrangThaiLocale,
  ngay,
  ngayGio,
  tenTrangThaiLocale,
  type LoaiNoiDung,
} from '@/lib/noiDung';

/**
 * Nội dung khác (docs/22 M13) — **một màn hình, ba loại**.
 *
 * Bản đồ màn hình của `docs/22` xếp M13 là một mục, không phải ba, và giao diện
 * đi theo: loại đang xem nằm trong URL (`?loai=bai-viet`), nên nhân viên gửi
 * link cho nhau và F5 vẫn ở đúng chỗ.
 *
 * **Cột trạng thái từng locale là cột quan trọng nhất.** "Chưa có" ở `vi` không
 * phải một ô trống — nó nghĩa là bản ghi này *không tồn tại* với khách đọc tiếng
 * Việt (chính sách không-fallback, `docs/02` mục 4).
 *
 * Khách sạn và điểm tham quan chưa có ở đây: hai bảng đó gắn vào sản phẩm và
 * chưa có đường đọc quản trị nào. Ghi ra để người sau không tưởng là bỏ sót.
 */
export default function NoiDungKhac() {
  return (
    <Suspense fallback={<main><div className="dang-tai" /></main>}>
      <NoiDung />
    </Suspense>
  );
}

function NoiDung() {
  const router = useRouter();
  const thamSo = useSearchParams();

  const thamSoLoai = thamSo.get('loai') ?? '';
  const loai: LoaiNoiDung = laLoaiNoiDung(thamSoLoai) ? thamSoLoai : 'diem-den';

  return (
    <main>
      <div className="dau-trang">
        <div>
          <h1>Nội dung khác</h1>
          <p className="phu">
            Điểm đến, bài viết và sự kiện. Cột ngôn ngữ nói rõ bản ghi nào{' '}
            <strong>chưa tồn tại</strong> với khách đọc thứ tiếng đó.
          </p>
        </div>
      </div>

      <nav className="tab">
        {LOAI_NOI_DUNG.map(([ma, ten]) => (
          <button
            key={ma}
            aria-current={loai === ma}
            onClick={() => router.push(`/noi-dung?loai=${ma}`)}
          >
            {ten}
          </button>
        ))}
      </nav>

      {loai === 'diem-den' && <DanhSachDiemDen />}
      {loai === 'bai-viet' && <DanhSachBaiViet />}
      {loai === 'su-kien' && <DanhSachSuKien />}
    </main>
  );
}

/* ------------------------------------------------------------ điểm đến */

function DanhSachDiemDen() {
  const { du_lieu, loi } = useNap<AdminDestination[]>(() =>
    adminApi().listAdminDestinations(),
  );

  if (loi) return <p className="loi">{loi}</p>;
  if (!du_lieu) return <KhungCho />;
  if (du_lieu.length === 0) return <p className="trong">Chưa có điểm đến nào.</p>;

  return (
    <table>
      <thead>
        <tr>
          <th>Mã</th>
          <th>Tên (bản nguồn)</th>
          <th>Miền</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {du_lieu.map((d) => (
          <tr key={d.id}>
            <td>
              <code>{d.code}</code>
            </td>
            <td>{d.name}</td>
            <td>{d.regionName}</td>
            <td>
              <Link href={`/noi-dung/diem-den/${d.id}`}>Sửa</Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/* ------------------------------------------------------------ bài viết */

function DanhSachBaiViet() {
  const { du_lieu, loi } = useNap<AdminPostPage>(() =>
    adminApi().listAdminPosts({ page: 0, size: 50 }),
  );

  if (loi) return <p className="loi">{loi}</p>;
  if (!du_lieu) return <KhungCho />;
  if (du_lieu.items.length === 0) {
    return <p className="trong">Chưa có bài viết nào.</p>;
  }

  return (
    <>
      <p className="phu">{du_lieu.totalItems} bài viết</p>
      <table>
        <thead>
          <tr>
            <th>Tiêu đề</th>
            <th>Ngôn ngữ</th>
            <th>Thẻ</th>
            <th>Đăng lúc</th>
            <th>Sửa lần cuối</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {du_lieu.items.map((b) => (
            <tr key={b.id}>
              <td>{b.title}</td>
              <td>
                <CotNgonNgu locales={b.locales} />
              </td>
              <td>{b.tags.map((t) => t.name).join(', ') || '—'}</td>
              <td>{ngay(b.publishedAt)}</td>
              <td>
                {ngayGio(b.lastModifiedAt)}
                {b.lastModifiedBy && <span className="phu"> · {b.lastModifiedBy}</span>}
              </td>
              <td>
                <Link href={`/noi-dung/bai-viet/${b.id}`}>Sửa</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

/* ------------------------------------------------------------ sự kiện */

function DanhSachSuKien() {
  const { du_lieu, loi } = useNap<AdminLecturePage>(() =>
    adminApi().listAdminLectures({ page: 0, size: 50 }),
  );

  if (loi) return <p className="loi">{loi}</p>;
  if (!du_lieu) return <KhungCho />;
  if (du_lieu.items.length === 0) {
    return <p className="trong">Chưa có buổi thuyết trình nào.</p>;
  }

  return (
    <>
      <p className="phu">
        {du_lieu.totalItems} buổi — gồm cả buổi <strong>đã diễn ra</strong>. Bề mặt khách
        chỉ hiện buổi sắp tới.
      </p>
      <table>
        <thead>
          <tr>
            <th>Ngày</th>
            <th>Tiêu đề</th>
            <th>Nơi</th>
            <th>Thị trường</th>
            <th>Chỗ</th>
            <th>Ngôn ngữ</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {du_lieu.items.map((s) => (
            <tr key={s.id}>
              <td>
                {ngay(s.eventDate)}
                {s.startTime && <span className="phu"> · {s.startTime}</span>}
              </td>
              <td>{s.title}</td>
              <td>
                {s.city}
                {s.venue && <span className="phu"> · {s.venue}</span>}
              </td>
              <td>{s.market}</td>
              {/* Hai vế, không phải một: nhân viên cần biết hạ sức chứa được
                  tới đâu, và con số đó là số đã đăng ký. */}
              <td>
                {s.seatsTaken}/{s.seats}
              </td>
              <td>
                <CotNgonNgu locales={s.locales} />
              </td>
              <td>
                <Link href={`/noi-dung/su-kien/${s.id}`}>Sửa</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

/* ------------------------------------------------------------ dùng chung */

function CotNgonNgu({ locales }: { locales: Record<string, never> | object }) {
  const muc = Object.entries(locales as Record<string, never>);
  return (
    <span className="hang" style={{ gap: '0.35rem' }}>
      {muc.map(([locale, trangThai]) => (
        <span key={locale} className={`nhan ${mauTrangThaiLocale(trangThai)}`}>
          {locale}: {tenTrangThaiLocale(trangThai)}
        </span>
      ))}
    </span>
  );
}

function KhungCho() {
  return (
    <div>
      {Array.from({ length: 4 }, (_, i) => (
        <div key={i} className="dang-tai" />
      ))}
    </div>
  );
}

/**
 * Nạp một lần, ba trạng thái.
 *
 * Gộp thành một hook vì ba danh sách khác nhau ở đúng lời gọi API — dựng ba lần
 * cùng một khối `useEffect` là ba chỗ để quên xử lý lỗi.
 */
function useNap<T>(goi: () => Promise<T>) {
  const [du_lieu, setDuLieu] = useState<T | null>(null);
  const [loi, setLoi] = useState('');

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const kq = await goi();
        if (conHieuLuc) setDuLieu(kq);
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(
            laKhongDuQuyen(ex)
              ? 'Vai trò của bạn không xem được nội dung này.'
              : await loiTiengViet(ex),
          );
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
    // `goi` dựng mới mỗi lần render nên không đưa vào danh sách phụ thuộc —
    // đưa vào là gọi API vô hạn.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { du_lieu, loi };
}
