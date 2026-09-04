'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import type { StaffProfile } from '@travel/api-client';
import { adminApi, laChuaDangNhap } from '@/lib/api';

const MUC = [
  ['/', 'Bảng điều khiển', 'M3 9.5 12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1V9.5Z'],
  ['/san-pham', 'Sản phẩm', 'M3 7l9-4 9 4-9 4-9-4Zm0 5l9 4 9-4M3 17l9 4 9-4'],
  ['/don', 'Đơn đặt', 'M6 2h9l5 5v15H6V2Zm9 0v5h5M9 12h7M9 16h7'],
  ['/bao-gia', 'Báo giá', 'M12 2v20M17 5.5c0-1.9-2.2-2.5-5-2.5s-5 .8-5 3 2.5 2.8 5 3.5 5 1.3 5 3.5-2.2 3-5 3-5-.6-5-2.5'],
] as const;

/**
 * Thanh điều hướng dọc bên trái.
 *
 * Bố cục cột dọc là quy ước chung của mọi bảng quản trị — AdminLTE, Tabler,
 * shadcn Admin, TailAdmin đều vậy — và nó không phải chuyện thẩm mỹ: menu dọc
 * mở rộng được xuống dưới khi thêm màn hình, còn menu ngang thì hết chỗ.
 *
 * Đây cũng là **chốt chặn phiên** của cả trang quản trị: gọi `/admin/me` một
 * lần, chưa đăng nhập thì đẩy về trang đăng nhập. Đặt ở đây thay vì lặp ở từng
 * màn hình vì lặp thì sẽ có màn hình quên, và màn hình quên đó hiện ra một trang
 * trống không giải thích gì.
 */
export function Canh() {
  const duongDan = usePathname();
  const router = useRouter();
  // `undefined` = chưa hỏi xong. Ba trạng thái trong một biến, để không phải đặt
  // state đồng bộ ngay trong thân effect — thứ gây dựng lại dây chuyền.
  const [nhanVien, setNhanVien] = useState<StaffProfile | null | undefined>(undefined);

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ho_so = await adminApi().hoSoNhanVien();
        if (conHieuLuc) {
          setNhanVien(ho_so);
        }
      } catch (loi) {
        if (laChuaDangNhap(loi)) {
          router.replace('/dang-nhap');
        } else if (conHieuLuc) {
          setNhanVien(null);
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [router]);

  async function dangXuat() {
    await adminApi().dangXuat();
    // replace chứ không push: bấm Back sau khi đăng xuất không được quay lại
    // màn hình có dữ liệu chưa xuất bản.
    router.replace('/dang-nhap');
    router.refresh();
  }

  return (
    <aside className="canh">
      <div className="canh-hieu">
        <span className="canh-dau">VR</span>
        <span>
          <strong>Vietnamrejser</strong>
          <em>Quản trị</em>
        </span>
      </div>

      <nav className="canh-menu">
        {MUC.map(([href, ten, d]) => {
          const dangO = href === '/' ? duongDan === '/' : duongDan.startsWith(href);
          return (
            <Link key={href} href={href} aria-current={dangO ? 'page' : undefined}>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d={d} />
              </svg>
              {ten}
            </Link>
          );
        })}
      </nav>

      <div className="canh-duoi">
        {nhanVien === undefined && <div className="dang-tai" style={{ opacity: 0.25 }} />}
        {nhanVien && (
          <>
            <div className="canh-nguoi">
              <span className="canh-chu-cai">{chuCai(nhanVien.displayName)}</span>
              <span>
                <strong>{nhanVien.displayName}</strong>
                <em>{nhanVien.roles.join(' · ')}</em>
              </span>
            </div>
            <button className="canh-thoat" onClick={dangXuat}>
              Đăng xuất
            </button>
          </>
        )}
      </div>
    </aside>
  );
}

function chuCai(ten: string): string {
  return ten
    .split(/\s+/)
    .slice(-2)
    .map((t) => t[0] ?? '')
    .join('')
    .toUpperCase();
}
