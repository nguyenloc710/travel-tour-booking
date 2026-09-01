import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Quản trị — Vietnamrejser',
};

/**
 * Trang quản trị dùng chung design system với trang khách (`@travel/ui`) nhưng
 * không dùng chung bố cục: nhân viên và khách có nhu cầu khác hẳn nhau.
 *
 * Không dùng Thymeleaf hay hệ giao diện thứ hai — docs/00 mục 2.1.e.
 */
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
