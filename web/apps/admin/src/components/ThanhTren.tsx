'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import type { StaffProfile } from '@travel/api-client';
import { adminApi, laChuaDangNhap } from '@/lib/api';

/**
 * Thanh trên cùng: điều hướng, người đang đăng nhập, nút đăng xuất.
 *
 * Nó cũng là **chốt chặn phiên** của cả trang quản trị: gọi `/admin/me` một lần,
 * chưa đăng nhập thì đẩy về trang đăng nhập. Đặt ở đây thay vì lặp ở từng màn
 * hình vì lặp thì sẽ có màn hình quên, và màn hình quên đó hiện ra một trang
 * trống không giải thích gì.
 */
export function ThanhTren() {
  const duongDan = usePathname();
  const router = useRouter();
  // `undefined` = chưa hỏi xong. Ba trạng thái trong một biến, để không phải
  // đặt state đồng bộ ngay trong thân effect — thứ gây dựng lại dây chuyền.
  const [nhanVien, setNhanVien] = useState<StaffProfile | null | undefined>(undefined);

  const trangDangNhap = duongDan === '/dang-nhap';

  useEffect(() => {
    if (trangDangNhap) {
      return;
    }
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
    // Người dùng rời trang giữa lúc chờ thì đừng đặt state vào một component đã
    // gỡ — React cảnh báo, và cảnh báo đó đúng.
    return () => {
      conHieuLuc = false;
    };
  }, [trangDangNhap, router]);

  async function dangXuat() {
    await adminApi().dangXuat();
    // replace chứ không push: bấm Back sau khi đăng xuất không được quay lại
    // màn hình có dữ liệu chưa xuất bản.
    router.replace('/dang-nhap');
    router.refresh();
  }

  if (trangDangNhap) {
    return null;
  }

  return (
    <header className="thanh-tren">
      <strong>Quản trị</strong>
      <nav>
        <Link href="/">Bảng điều khiển</Link>
        <Link href="/san-pham">Sản phẩm</Link>
      </nav>
      <div className="day-phai">
        {nhanVien === undefined && '…'}
        {nhanVien && (
          <>
            {nhanVien.displayName} · {nhanVien.roles.join(', ')}{' '}
            <button className="phu" onClick={dangXuat} style={{ marginLeft: '0.5rem' }}>
              Đăng xuất
            </button>
          </>
        )}
      </div>
    </header>
  );
}
