'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { adminApi, loiTiengViet } from '@/lib/api';

/**
 * Đăng nhập nhân viên.
 *
 * Phản hồi thành công **không mang token**: phiên nằm trong cookie `HttpOnly`,
 * thứ mã chèn vào trang không đọc được (docs/22 mục 9). Trang này vì thế không
 * lưu gì vào `localStorage` và cũng không có gì để lưu.
 */
export default function DangNhap() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [matKhau, setMatKhau] = useState('');
  const [loi, setLoi] = useState('');
  const [dangGui, setDangGui] = useState(false);

  async function gui(e: React.FormEvent) {
    e.preventDefault();
    setLoi('');
    setDangGui(true);
    try {
      await adminApi().createSession({ loginRequest: { email, password: matKhau } });
      router.replace('/');
      router.refresh();
    } catch (ex) {
      // Một câu cho cả sai email lẫn sai mật khẩu — phân biệt hai cái là cho
      // phép dò xem địa chỉ nào có trong hệ thống. Máy chủ đã trả cùng một mã;
      // chỗ này chỉ giữ nguyên tính chất đó.
      const cau = await loiTiengViet(ex);
      setLoi(
        cau.startsWith('Lỗi 401')
          ? 'Email hoặc mật khẩu không đúng, hoặc tài khoản đã bị vô hiệu hoá.'
          : cau,
      );
    } finally {
      setDangGui(false);
    }
  }

  return (
    <div className="dang-nhap">
      <div className="the">
        <h1>Đăng nhập</h1>
        <p className="phu">Trang quản trị Vietnamrejser.</p>

        <form onSubmit={gui}>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <label htmlFor="mat-khau">Mật khẩu</label>
          <input
            id="mat-khau"
            type="password"
            autoComplete="current-password"
            required
            minLength={8}
            value={matKhau}
            onChange={(e) => setMatKhau(e.target.value)}
          />

          {loi && <p className="loi">{loi}</p>}

          <p style={{ marginBottom: 0 }}>
            <button type="submit" disabled={dangGui} style={{ width: '100%' }}>
              {dangGui ? 'Đang kiểm tra…' : 'Đăng nhập'}
            </button>
          </p>
        </form>
      </div>
    </div>
  );
}
