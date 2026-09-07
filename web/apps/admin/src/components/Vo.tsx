'use client';

import { usePathname } from 'next/navigation';
import { Canh } from '@/components/Canh';

/**
 * Vỏ của trang quản trị: thanh dọc bên trái, nội dung bên phải.
 *
 * Trang đăng nhập **không** có thanh dọc — lúc đó chưa biết người dùng là ai,
 * nên hiện một menu điều hướng là hứa hẹn thứ họ chưa bấm được.
 */
export function Vo({ children }: { children: React.ReactNode }) {
  const duongDan = usePathname();

  if (duongDan === '/dang-nhap') {
    return <>{children}</>;
  }

  return (
    <div className="vo">
      <Canh />
      <div className="vung">{children}</div>
    </div>
  );
}
