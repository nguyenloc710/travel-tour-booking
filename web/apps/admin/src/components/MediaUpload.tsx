'use client';

import { useRef, useState } from 'react';
import {
  MediaKind,
  MediaSource,
  MediaUploadUrlRequestContentTypeEnum,
  MediaUploadUrlRequestFolderEnum,
  type AdminMedia,
} from '@travel/api-client';
import { adminApi, loiTiengViet } from '@/lib/api';

/**
 * Tải một tệp lên kho, ba bước — ADR-011 mục 4, ADR-013.
 *
 * ```
 *   1. xin URL đã ký          POST /admin/media/upload-url
 *   2. PUT thẳng lên kho      trình duyệt → MinIO, KHÔNG qua API
 *   3. ghi bản ghi            POST /admin/media
 * ```
 *
 * Bước 2 cố tình không đi qua API: đẩy tệp qua backend nghĩa là mỗi tệp đi qua bộ
 * nhớ của nó hai lần, và một tour có bốn mươi ảnh.
 *
 * **Video thì làm sáu bước, không phải ba** — và người dùng không phải biết:
 * trình duyệt bắt lấy khung hình đầu làm ảnh bìa, tải ảnh bìa lên như một bản ghi
 * `IMAGE` riêng, rồi mới tải video kèm `posterAssetId`. Ràng buộc
 * `ck_media_video` của V9 đòi ảnh bìa, và bắt biên tập viên tự chuẩn bị một tấm
 * cho mỗi video là thêm một việc mà máy làm được.
 *
 * **Hai ô bắt buộc mà người dùng hay tưởng là tuỳ chọn**, nên chúng nằm ngay
 * trong biểu mẫu này chứ không giấu sau một nút "nâng cao":
 *
 * - `alt` — tệp không có chữ thay ở locale nào thì **biến mất** ở locale đó, đúng
 *   luật không fallback. Ở đây nhập bản `da` (ngôn ngữ nguồn, ADR-004).
 * - `licenceRef` — tệp không tự chụp mà thiếu chứng từ là thứ cơ sở dữ liệu từ
 *   chối thẳng (`ck_media_licence`), vì ảnh tạm là thứ ở lại lâu nhất.
 */
export function MediaUpload({
  folder = MediaUploadUrlRequestFolderEnum.Tour,
  allowVideo = false,
  onUploaded,
}: {
  folder?: MediaUploadUrlRequestFolderEnum;
  allowVideo?: boolean;
  onUploaded: (tep: AdminMedia, alt: string) => void;
}) {
  const oTep = useRef<HTMLInputElement>(null);
  const [tep, setTep] = useState<File | null>(null);
  const [alt, setAlt] = useState('');
  const [source, setSource] = useState<MediaSource>(MediaSource.Self);
  const [licenceRef, setLicenceRef] = useState('');
  const [loi, setLoi] = useState('');
  const [dangTai, setDangTai] = useState(false);

  const laVideo = tep !== null && tep.type.startsWith('video/');
  const canChungTu = source !== MediaSource.Self;
  const guiDuoc = tep !== null && alt.trim() !== '' && (!canChungTu || licenceRef.trim() !== '');

  const nhanTep = allowVideo
    ? 'image/jpeg,image/png,image/webp,image/avif,video/mp4,video/webm'
    : 'image/jpeg,image/png,image/webp,image/avif';

  /** Ba bước của một tệp: xin URL, PUT lên kho, ghi bản ghi. */
  async function tai(
    tepCanTai: File,
    kind: MediaKind,
    khung: { width: number; height: number },
    chuThay: string,
    them?: { durationSeconds: number; posterAssetId: string },
  ): Promise<AdminMedia> {
    const ve = await adminApi().createMediaUploadUrl({
      mediaUploadUrlRequest: {
        fileName: tepCanTai.name,
        contentType: tepCanTai.type as MediaUploadUrlRequestContentTypeEnum,
        byteSize: tepCanTai.size,
        folder,
      },
    });

    // Gọi thẳng kho, KHÔNG qua client sinh ra: đây không phải endpoint của API và
    // nó không nhận cookie phiên của chúng ta.
    const len = await fetch(ve.uploadUrl, { method: 'PUT', body: tepCanTai });
    if (!len.ok) {
      throw new Error(`kho trả ${len.status}`);
    }

    return adminApi().createMedia({
      adminMediaCreate: {
        kind,
        path: ve.path,
        width: khung.width,
        height: khung.height,
        byteSize: tepCanTai.size,
        source,
        licenceRef: canChungTu ? licenceRef.trim() : undefined,
        alt: chuThay,
        contentType: kind === MediaKind.Video ? tepCanTai.type : undefined,
        durationSeconds: them?.durationSeconds,
        posterAssetId: them?.posterAssetId,
      },
    });
  }

  async function guiLen() {
    if (tep === null) {
      return;
    }
    setLoi('');
    setDangTai(true);
    try {
      const chu = alt.trim();
      let ketQua: AdminMedia;

      if (tep.type.startsWith('video/')) {
        const doc = await docVideo(tep);
        // Ảnh bìa là một bản ghi riêng, có giấy phép và alt của chính nó — nên nó
        // dùng lại đúng `alt` của video: cùng một cảnh, cùng một câu mô tả.
        const bia = await tai(doc.poster, MediaKind.Image, doc.khung, chu);
        ketQua = await tai(tep, MediaKind.Video, doc.khung, chu, {
          durationSeconds: doc.durationSeconds,
          posterAssetId: bia.id,
        });
      } else {
        ketQua = await tai(tep, MediaKind.Image, await docAnh(tep), chu);
      }

      onUploaded(ketQua, chu);
      setTep(null);
      setAlt('');
      setLicenceRef('');
      if (oTep.current) {
        oTep.current.value = '';
      }
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangTai(false);
    }
  }

  return (
    <div className="the" style={{ marginTop: '0.75rem' }}>
      <label htmlFor="tep-media">Chọn {allowVideo ? 'ảnh hoặc video' : 'ảnh'}</label>
      <input
        id="tep-media"
        ref={oTep}
        type="file"
        accept={nhanTep}
        onChange={(e) => setTep(e.target.files?.[0] ?? null)}
      />
      <p className="phu" style={{ marginTop: '0.25rem' }}>
        Ảnh: JPEG, PNG, WebP hoặc AVIF, tối đa 10 MB.
        {allowVideo && ' Video: MP4 hoặc WebM, tối đa 200 MB — ảnh bìa lấy tự động từ khung đầu.'}
      </p>

      <label htmlFor="media-alt">Chữ thay tệp — tiếng Đan</label>
      <input id="media-alt" value={alt} onChange={(e) => setAlt(e.target.value)} />
      <p className="phu" style={{ marginTop: '0.25rem' }}>
        Đây là <strong>nội dung phải dịch</strong>. Chưa có bản tiếng Việt thì tệp
        không hiện ở trang tiếng Việt.
        {laVideo && ' Ảnh bìa dùng lại đúng câu này.'}
      </p>

      <label htmlFor="media-nguon">Nguồn</label>
      <select
        id="media-nguon"
        value={source}
        onChange={(e) => setSource(e.target.value as MediaSource)}
      >
        <option value={MediaSource.Self}>Tự chụp, tự quay</option>
        <option value={MediaSource.Partner}>Của đối tác</option>
        <option value={MediaSource.Purchased}>Mua bản quyền</option>
        <option value={MediaSource.Customer}>Khách gửi</option>
      </select>

      {canChungTu && (
        <>
          <label htmlFor="media-giay-phep">Chứng từ giấy phép</label>
          <input
            id="media-giay-phep"
            placeholder="số hợp đồng, mã đơn mua, hoặc đường dẫn tới thư cho phép"
            value={licenceRef}
            onChange={(e) => setLicenceRef(e.target.value)}
          />
          <p className="phu" style={{ marginTop: '0.25rem' }}>
            Bắt buộc với tệp không tự chụp. Phải là thứ tra lại được sau hai năm.
          </p>
        </>
      )}

      {loi && <p className="loi">{loi}</p>}

      <p style={{ marginTop: '0.75rem' }}>
        <button type="button" disabled={!guiDuoc || dangTai} onClick={guiLen}>
          {dangTai ? 'Đang tải lên…' : 'Tải lên'}
        </button>
      </p>
    </div>
  );
}

/** Kích thước thật của ảnh, đọc bằng trình duyệt chứ không tin phần mở rộng tệp. */
async function docAnh(tep: File): Promise<{ width: number; height: number }> {
  const anh = await createImageBitmap(tep);
  try {
    return { width: anh.width, height: anh.height };
  } finally {
    anh.close();
  }
}

/**
 * Đọc video: khung hình, thời lượng, và **ảnh bìa lấy từ khung đầu**.
 *
 * Tua tới 0,1 giây chứ không lấy đúng giây 0: khung đầu của nhiều video là một
 * khung đen, và một ảnh bìa đen thì cũng vô dụng như không có.
 */
async function docVideo(
  tep: File,
): Promise<{ khung: { width: number; height: number }; durationSeconds: number; poster: File }> {
  const url = URL.createObjectURL(tep);
  const video = document.createElement('video');
  video.muted = true;
  video.preload = 'metadata';
  video.src = url;

  try {
    await new Promise<void>((xong, hong) => {
      video.onloadeddata = () => xong();
      video.onerror = () => hong(new Error('trình duyệt không đọc được video này'));
    });

    await new Promise<void>((xong) => {
      video.onseeked = () => xong();
      video.currentTime = Math.min(0.1, video.duration || 0.1);
    });

    const khung = { width: video.videoWidth, height: video.videoHeight };
    const canvas = document.createElement('canvas');
    canvas.width = khung.width;
    canvas.height = khung.height;
    canvas.getContext('2d')?.drawImage(video, 0, 0);

    const blob = await new Promise<Blob | null>((xong) =>
      canvas.toBlob((b) => xong(b), 'image/jpeg', 0.85),
    );
    if (blob === null) {
      throw new Error('không dựng được ảnh bìa từ video');
    }

    return {
      khung,
      durationSeconds: Math.max(1, Math.round(video.duration)),
      poster: new File([blob], tep.name.replace(/\.[^.]+$/, '') + '-bia.jpg', {
        type: 'image/jpeg',
      }),
    };
  } finally {
    URL.revokeObjectURL(url);
  }
}
