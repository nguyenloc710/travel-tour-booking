'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  MediaKind,
  MediaUploadUrlRequestFolderEnum,
  type AdminMedia,
} from '@travel/api-client';
import { laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { useFieldErrors } from '@/lib/useFieldErrors';
import { MediaUpload } from './MediaUpload';

/**
 * Danh sách tệp **có thứ tự** của một thực thể — bộ ảnh sản phẩm, hoặc ảnh và
 * video của điểm đến.
 *
 * Một component cho hai bề mặt, vì chúng chỉ khác nhau hai thứ: có nhận video
 * hay không, và gọi endpoint nào. Cả hai đi vào bằng tham số.
 *
 * **Thứ tự lưu bằng cách gửi lại cả mảng.** Không có nút "lưu thứ tự" riêng: đổi
 * chỗ là một thao tác hoàn chỉnh, và một danh sách đã đổi chỗ mà chưa lưu là một
 * trạng thái không ai muốn nhìn thấy trên trang khách.
 */
export function MediaList({
  nap,
  luu,
  allowVideo = false,
  folder,
  tieuDe,
}: {
  nap: () => Promise<AdminMedia[]>;
  luu: (assetIds: string[]) => Promise<AdminMedia[]>;
  allowVideo?: boolean;
  folder: MediaUploadUrlRequestFolderEnum;
  tieuDe: string;
}) {
  const [ds, setDs] = useState<AdminMedia[] | null>(null);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState('');
  const [dangLuu, setDangLuu] = useState(false);
  const loiO = useFieldErrors();

  const doc = useCallback(async () => {
    try {
      const ketQua = await nap();
      setLoi('');
      setDs(ketQua);
    } catch (ex) {
      setDs([]);
      setLoi(await loiTiengViet(ex));
    }
  }, [nap]);

  useEffect(() => {
    void (async () => {
      await doc();
    })();
  }, [doc]);

  async function ghi(thuTuMoi: AdminMedia[]) {
    setLoi('');
    setXong('');
    loiO.clear();
    setDangLuu(true);
    try {
      setDs(await luu(thuTuMoi.map((m) => m.id)));
      setXong('Đã lưu.');
    } catch (ex) {
      setLoi(laKhongDuQuyen(ex) ? 'Chỉ vai trò EDITOR hoặc ADMIN sửa được danh sách này.' : await loiTiengViet(ex));
      await loiO.show(ex);
      await doc();
    } finally {
      setDangLuu(false);
    }
  }

  function doiCho(i: number, buoc: -1 | 1) {
    if (ds === null) {
      return;
    }
    const j = i + buoc;
    if (j < 0 || j >= ds.length) {
      return;
    }
    // Đổi chỗ bằng hai phép gán tường minh chứ không destructuring: `tsconfig`
    // bật `noUncheckedIndexedAccess`, nên `moi[j]` là `AdminMedia | undefined`.
    const moi = [...ds];
    const a = moi[i];
    const b = moi[j];
    if (a === undefined || b === undefined) {
      return;
    }
    moi[i] = b;
    moi[j] = a;
    void ghi(moi);
  }

  function go(i: number) {
    if (ds === null) {
      return;
    }
    void ghi(ds.filter((_, j) => j !== i));
  }

  if (ds === null) {
    return <div className="dang-tai" style={{ height: '10rem' }} />;
  }

  return (
    <>
      <h2>{tieuDe}</h2>
      <p className="phu">
        Thứ tự ở đây là thứ tự khách thấy. Gỡ một tệp khỏi danh sách{' '}
        <strong>không xoá tệp</strong> — chứng từ giấy phép của nó ở lại, và tệp có
        thể đang dùng ở chỗ khác.
      </p>

      {ds.length === 0 && <p>Chưa có tệp nào. Dùng khối bên dưới để thêm.</p>}
      {loiO.errorFor('assetIds')}

      {ds.map((m, i) => (
        <div className="the" key={m.id} style={{ marginTop: '0.75rem' }}>
          <div className="hang" style={{ alignItems: 'flex-start', gap: '1rem' }}>
            {/* Ảnh xem trước để nguyên `img`: đây là trang quản trị, không phải
                trang khách, nên không cần bộ tối ưu của next/image. */}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={m.kind === MediaKind.Video ? (m.posterUrl ?? m.url) : m.url}
              alt=""
              width={160}
              style={{ maxWidth: '10rem', height: 'auto', border: '1px solid #ddd' }}
            />
            <div style={{ flex: 1 }}>
              <p style={{ margin: 0 }}>
                <strong>
                  {i + 1}. {m.kind === MediaKind.Video ? 'Video' : 'Ảnh'}
                </strong>
                {m.kind === MediaKind.Video && m.durationSeconds !== undefined && (
                  <> · {m.durationSeconds} giây</>
                )}
                {' · '}
                {m.width}×{m.height}
              </p>
              <p className="phu" style={{ margin: '0.25rem 0 0', wordBreak: 'break-all' }}>
                {m.path}
              </p>
              {/* Locale nào không có `alt` thì tệp này vô hình ở đó — nói ra chứ
                  không để biên tập viên tự phát hiện bằng cách mở trang khách. */}
              <p
                className={m.translatedLocales.includes('vi') ? 'phu' : 'loi'}
                style={{ margin: '0.25rem 0 0' }}
              >
                Có chữ thay ở: {m.translatedLocales.join(', ') || 'chưa có locale nào'}
                {!m.translatedLocales.includes('vi') && ' — chưa hiện ở trang tiếng Việt'}
              </p>
            </div>
            <div className="hang" style={{ gap: '0.25rem' }}>
              <button type="button" disabled={i === 0 || dangLuu} onClick={() => doiCho(i, -1)}>
                ↑
              </button>
              <button
                type="button"
                disabled={i === ds.length - 1 || dangLuu}
                onClick={() => doiCho(i, 1)}
              >
                ↓
              </button>
              <button type="button" className="phu" disabled={dangLuu} onClick={() => go(i)}>
                Gỡ
              </button>
            </div>
          </div>
        </div>
      ))}

      {loi && <p className="loi">{loi}</p>}
      {xong && <p className="xong">{xong}</p>}

      <h3 style={{ marginTop: '1.5rem' }}>Thêm tệp</h3>
      <MediaUpload
        folder={folder}
        allowVideo={allowVideo}
        onUploaded={(tep) => {
          void ghi([...ds, tep]);
        }}
      />
    </>
  );
}
