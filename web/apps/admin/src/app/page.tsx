import { locales, markets, t } from '@travel/i18n';

/**
 * Khung trống của trang quản trị.
 *
 * Chức năng thật — CRUD sản phẩm, bảng điều khiển dịch thuật, quản lý ngày khởi
 * hành, đơn đặt — nằm ở giai đoạn G4 và cần `docs/22-trang-quan-tri.md`, chưa
 * viết. Ở đây chỉ đủ để `pnpm build` chạy được và chứng minh workspace đã nối
 * đúng ba package dùng chung.
 */
export default function AdminHome() {
  return (
    <main style={{ padding: '1.5rem', fontFamily: 'system-ui, sans-serif' }}>
      <h1>Trang quản trị</h1>
      <p>Chưa có chức năng. Đặc tả nằm ở docs/22, viết ở giai đoạn G4.</p>

      <h2>Cấu hình đang nạp được</h2>
      <ul>
        <li>Ngôn ngữ: {locales.join(', ')}</li>
        <li>Thị trường: {markets.join(', ')}</li>
        <li>Chuỗi thử tiếng Đan: {t('da', 'site.tagline')}</li>
        <li>Chuỗi thử tiếng Việt: {t('vi', 'site.tagline')}</li>
      </ul>

      <p>
        Thử phông chữ — cả hai dòng phải hiện đủ dấu:
        <br />
        <strong>Strand og øer på tværs</strong>
        <br />
        <strong>Điểm cuối · Hội An · Vịnh Hạ Long</strong>
      </p>
    </main>
  );
}
