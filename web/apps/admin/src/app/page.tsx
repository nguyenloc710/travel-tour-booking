'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import type { TranslationCoverageRow, TranslationQueueItem } from '@travel/api-client';
import { adminApi, laChuaDangNhap, loiTiengViet } from '@/lib/api';

/**
 * Bảng điều khiển (docs/22 M1) — bản tối thiểu: hàng đợi dịch và bảng độ phủ.
 *
 * Ba việc còn lại của M1 — đơn quá hạn xác nhận, báo giá sắp hết hạn, chuyến
 * thiếu khách sắp tới hạn chốt — chưa có endpoint nào, nên chưa hiện. Không dựng
 * ô giả để trông cho đủ: một con số bịa trên bảng điều khiển tệ hơn một ô trống,
 * vì người ta sẽ tin nó.
 */
export default function BangDieuKhien() {
  const [hangDoi, setHangDoi] = useState<TranslationQueueItem[] | null>(null);
  const [doPhu, setDoPhu] = useState<TranslationCoverageRow[] | null>(null);
  const [loi, setLoi] = useState('');

  useEffect(() => {
    const api = adminApi();
    Promise.all([api.hangDoiDich({ limit: 20 }), api.doPhuDich({})])
      .then(([q, c]) => {
        setHangDoi(q);
        setDoPhu(c);
      })
      .catch(async (ex) => {
        if (!laChuaDangNhap(ex)) {
          setLoi(await loiTiengViet(ex));
        }
      });
  }, []);

  return (
    <main>
      <h1>Bảng điều khiển</h1>
      <p className="phu">Việc cần làm hôm nay.</p>

      {loi && <p className="loi">{loi}</p>}

      <h2>Hàng đợi dịch</h2>
      <p className="phu">
        Bản tiếng Đan đã xuất bản nhưng bản tiếng Việt còn thiếu hoặc đã cũ. Xếp theo
        tác động doanh thu: tour đang bán lên trước.
      </p>

      {hangDoi === null && !loi && <KhungXuong dong={3} />}

      {hangDoi?.length === 0 && (
        <p className="trong">Không còn việc dịch nào đang chờ.</p>
      )}

      {hangDoi && hangDoi.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Ưu tiên</th>
              <th>Loại</th>
              <th>Tiêu đề bản nguồn</th>
              <th>Tình trạng</th>
              <th>Nguồn sửa lần cuối</th>
            </tr>
          </thead>
          <tbody>
            {hangDoi.map((v) => (
              <tr key={`${v.entityType}-${v.id}-${v.locale}`}>
                <td>{v.priority}</td>
                <td>{v.entityType === 'PRODUCT' ? 'Sản phẩm' : 'Bài viết'}</td>
                <td>
                  {v.entityType === 'PRODUCT' ? (
                    <Link href={`/san-pham/${v.id}`}>{v.sourceTitle}</Link>
                  ) : (
                    v.sourceTitle
                  )}
                </td>
                <td>
                  <span className={`nhan ${v.gap === 'MISSING' ? 'vang' : 'xam'}`}>
                    {v.gap === 'MISSING' ? 'Chưa dịch' : 'Đã cũ'}
                  </span>
                </td>
                <td>{ngay(v.sourceLastModifiedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Độ phủ dịch</h2>
      <p className="phu">
        Hai con số nói hai chuyện: <strong>đã dịch</strong> và <strong>còn hạn</strong>.
        Dịch đủ mà nhiều bản đã cũ nghĩa là nội dung đang trôi.
      </p>

      {doPhu === null && !loi && <KhungXuong dong={2} />}

      {doPhu && (
        <table>
          <thead>
            <tr>
              <th>Loại</th>
              <th>Ngôn ngữ</th>
              <th>Cần dịch</th>
              <th>Đã dịch</th>
              <th>Còn hạn</th>
            </tr>
          </thead>
          <tbody>
            {doPhu.map((r) => (
              <tr key={`${r.entityType}-${r.locale}`}>
                <td>{r.entityType === 'PRODUCT' ? 'Sản phẩm' : 'Bài viết'}</td>
                <td>{r.locale}</td>
                <td>{r.total}</td>
                <td>
                  {r.translated} {phanTram(r.translated, r.total)}
                </td>
                <td>
                  {r.upToDate} {phanTram(r.upToDate, r.total)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}

/** Khung xương, không phải vòng xoay — docs/20 mục 4. */
function KhungXuong({ dong }: { dong: number }) {
  return (
    <div>
      {Array.from({ length: dong }, (_, i) => (
        <div key={i} className="dang-tai" />
      ))}
    </div>
  );
}

/** API trả số đếm, không trả phần trăm — chia và làm tròn là việc ở đây. */
function phanTram(phan: number, tong: number): string {
  if (tong === 0) {
    return '';
  }
  return `(${Math.round((phan / tong) * 100)}%)`;
}

function ngay(d: Date | undefined): string {
  return d ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short' }).format(d) : '—';
}
