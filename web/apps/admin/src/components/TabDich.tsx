'use client';

import { useState } from 'react';
import type {
  AdminProductDetail,
  AdminProductTranslation,
  TranslationStatus,
} from '@travel/api-client';
import { adminApi, loiTiengViet } from '@/lib/api';

/** Sáu trường của biểu mẫu dịch. Kiểu tường minh chứ không `Record<string, string>`:
 *  chỉ mục vào record trả `string | undefined`, và ô nhập không nhận `undefined`. */
type GiaTri = {
  slug: string;
  title: string;
  shortDescription: string;
  heroImageAlt: string;
  longDescription: string;
  whyChooseThis: string;
};

const TRUONG = [
  ['slug', 'Slug (đường dẫn)', 'input'],
  ['title', 'Tiêu đề', 'input'],
  ['shortDescription', 'Mô tả ngắn', 'textarea'],
  ['heroImageAlt', 'Chữ thay ảnh đầu trang', 'input'],
] as const;

/**
 * Màn hình dịch song song (docs/22 M11).
 *
 * Bản nguồn **bên trái, chỉ đọc**; ô nhập bên phải; **từng trường một**, cạnh
 * nhau theo hàng — không phải hai khối văn bản lớn. Dịch cả khối là bỏ sót
 * trường, và không ai thấy.
 *
 * **Ô nhập không điền sẵn bằng bản nguồn.** Người dịch sẽ sửa qua loa và để sót
 * nguyên câu tiếng Đan. Cũng không có nút dịch máy — docs/02 mục 8.
 */
export function TabDich({
  sp,
  banDich,
  napLai,
}: {
  sp: AdminProductDetail;
  banDich: AdminProductTranslation[];
  napLai: () => Promise<void>;
}) {
  const nguon = banDich.find((t) => t.isSource);
  const dichs = banDich.filter((t) => !t.isSource);

  // Locale đích chưa có bản dịch nào thì vẫn phải mở được ô nhập, nếu không thì
  // không có đường tạo bản dịch đầu tiên.
  const localeDich = dichs.length > 0 ? dichs.map((t) => t.locale) : ['vi'];
  const [dangXem, setDangXem] = useState(localeDich[0] ?? 'vi');

  const dich = dichs.find((t) => t.locale === dangXem);

  if (!nguon) {
    return <p className="loi">Sản phẩm không có bản ngôn ngữ nguồn — dữ liệu hỏng.</p>;
  }

  return (
    <>
      <div className="hang">
        {localeDich.map((l) => (
          <button key={l} className={l === dangXem ? '' : 'phu'} onClick={() => setDangXem(l)}>
            {nguon.locale} → {l}
          </button>
        ))}
      </div>

      {dich?.outdated && (
        <p className="loi" style={{ background: '#fdf3e0', borderColor: '#e6cf9e', color: '#8a5a00' }}>
          Bản nguồn đã sửa sau lần dịch gần nhất. Đối chiếu lại từng trường rồi bấm
          &laquo;Đánh dấu đã dịch&raquo;.
        </p>
      )}

      <BieuMauDich
        key={dangXem}
        productId={sp.id}
        nguon={nguon}
        dich={dich}
        localeDich={dangXem}
        napLai={napLai}
      />

      <BieuMauNguon key={`nguon-${nguon.locale}`} productId={sp.id} nguon={nguon} napLai={napLai} />
    </>
  );
}

function BieuMauDich({
  productId,
  nguon,
  dich,
  localeDich,
  napLai,
}: {
  productId: string;
  nguon: AdminProductTranslation;
  dich: AdminProductTranslation | undefined;
  localeDich: string;
  napLai: () => Promise<void>;
}) {
  const [gt, setGt] = useState<GiaTri>({
    slug: dich?.slug ?? '',
    title: dich?.title ?? '',
    shortDescription: dich?.shortDescription ?? '',
    heroImageAlt: dich?.heroImageAlt ?? '',
    longDescription: (dich?.longDescription ?? []).join('\n\n'),
    whyChooseThis: (dich?.whyChooseThis ?? []).join('\n'),
  });
  const [trangThai, setTrangThai] = useState<TranslationStatus>(dich?.status ?? 'DRAFT');
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState('');
  const [dangLuu, setDangLuu] = useState(false);

  const slugCoDau = /[^a-z0-9-]/.test(gt.slug);

  async function luu(moiTrangThai: TranslationStatus) {
    setLoi('');
    setXong('');
    setDangLuu(true);
    try {
      await adminApi().saveProductTranslation({
        id: productId,
        locale: localeDich as never,
        adminProductTranslationInput: {
          slug: gt.slug,
          title: gt.title,
          shortDescription: gt.shortDescription,
          longDescription: doanVan(gt.longDescription),
          whyChooseThis: dong(gt.whyChooseThis),
          heroImageAlt: gt.heroImageAlt,
          status: moiTrangThai,
        },
      });
      setTrangThai(moiTrangThai);
      setXong(moiTrangThai === 'DRAFT' ? 'Đã lưu nháp.' : 'Đã lưu.');
      await napLai();
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  return (
    <>
      <h2>
        Dịch {nguon.locale} → {localeDich}
      </h2>

      <div className="song-song">
        <div>
          <strong className="phu">Bản nguồn ({nguon.locale}) — chỉ đọc</strong>
        </div>
        <div>
          <strong className="phu">Bản dịch ({localeDich})</strong>
        </div>

        {TRUONG.map(([ten, nhan, kieu]) => (
          <Cap
            key={ten}
            nhan={nhan}
            batBuoc
            nguon={String(nguon[ten] ?? '')}
            giaTri={gt[ten]}
            kieu={kieu}
            doi={(v) => setGt({ ...gt, [ten]: v })}
            canhBao={ten === 'slug' && slugCoDau ? 'Slug chỉ được dùng a–z, 0–9 và dấu gạch nối.' : ''}
          />
        ))}

        <Cap
          nhan="Mô tả dài (cách nhau một dòng trống)"
          batBuoc
          nguon={nguon.longDescription.join('\n\n')}
          giaTri={gt.longDescription}
          kieu="textarea"
          doi={(v) => setGt({ ...gt, longDescription: v })}
        />

        <Cap
          nhan="Vì sao chọn tour này (mỗi dòng một ý, 3–7 ý)"
          batBuoc
          nguon={nguon.whyChooseThis.join('\n')}
          giaTri={gt.whyChooseThis}
          kieu="textarea"
          doi={(v) => setGt({ ...gt, whyChooseThis: v })}
        />
      </div>

      {loi && <p className="loi">{loi}</p>}
      {xong && <p className="xong">{xong}</p>}

      <div className="hang" style={{ marginTop: '1rem' }}>
        {/* Lưu nháp tách khỏi xuất bản: một tour có nhiều trường, mất việc giữa
            chừng là mất buổi làm (docs/22 mục 4.2). */}
        <button className="phu" disabled={dangLuu} onClick={() => luu('DRAFT')}>
          Lưu nháp
        </button>
        {/* "Đánh dấu đã dịch" tách khỏi "xuất bản": dịch xong khác với được lên
            website. */}
        <button className="phu" disabled={dangLuu} onClick={() => luu('TRANSLATED')}>
          Đánh dấu đã dịch
        </button>
        <button disabled={dangLuu} onClick={() => luu('PUBLISHED')}>
          Xuất bản
        </button>
        <span className="phu">Trạng thái hiện tại: {trangThai}</span>
      </div>
    </>
  );
}

/** Sửa chính bản nguồn — quyền khác hẳn, nên đặt tách hẳn ở dưới. */
function BieuMauNguon({
  productId,
  nguon,
  napLai,
}: {
  productId: string;
  nguon: AdminProductTranslation;
  napLai: () => Promise<void>;
}) {
  const [mo, setMo] = useState(false);
  const [tieuDe, setTieuDe] = useState(nguon.title);
  const [moTa, setMoTa] = useState(nguon.shortDescription);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState('');

  async function luu() {
    setLoi('');
    setXong('');
    try {
      await adminApi().saveProductTranslation({
        id: productId,
        locale: nguon.locale as never,
        adminProductTranslationInput: {
          slug: nguon.slug,
          title: tieuDe,
          shortDescription: moTa,
          longDescription: nguon.longDescription,
          whyChooseThis: nguon.whyChooseThis,
          heroImageAlt: nguon.heroImageAlt,
          status: nguon.status,
        },
      });
      setXong('Đã lưu bản nguồn. Bản dịch nay tính là đã cũ cho tới khi dịch lại.');
      await napLai();
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    }
  }

  return (
    <>
      <h2>Sửa bản nguồn ({nguon.locale})</h2>
      <p className="phu">
        Chỉ vai trò <strong>EDITOR</strong> và <strong>ADMIN</strong> sửa được bản
        nguồn. Sửa xong thì mọi bản dịch quay lại hàng đợi vì chúng đã cũ so với
        nguồn.
      </p>

      {!mo ? (
        <button className="phu" onClick={() => setMo(true)}>
          Mở sửa bản nguồn
        </button>
      ) : (
        <>
          <label htmlFor="nguon-title">Tiêu đề</label>
          <input id="nguon-title" value={tieuDe} onChange={(e) => setTieuDe(e.target.value)} />

          <label htmlFor="nguon-mota">Mô tả ngắn</label>
          <textarea id="nguon-mota" value={moTa} onChange={(e) => setMoTa(e.target.value)} />

          {loi && <p className="loi">{loi}</p>}
          {xong && <p className="xong">{xong}</p>}

          <p>
            <button onClick={luu}>Lưu bản nguồn</button>
          </p>
        </>
      )}
    </>
  );
}

function Cap({
  nhan,
  nguon,
  giaTri,
  kieu,
  doi,
  batBuoc,
  canhBao,
}: {
  nhan: string;
  nguon: string;
  giaTri: string;
  kieu: 'input' | 'textarea';
  doi: (v: string) => void;
  batBuoc?: boolean;
  canhBao?: string;
}) {
  return (
    <>
      <div>
        <label>
          {nhan} {batBuoc && <span className="phu">· bắt buộc</span>}
        </label>
        {kieu === 'input' ? (
          <input className="chi-doc" readOnly value={nguon} />
        ) : (
          <textarea className="chi-doc" readOnly value={nguon} />
        )}
      </div>
      <div>
        <label>&nbsp;</label>
        {kieu === 'input' ? (
          <input value={giaTri} onChange={(e) => doi(e.target.value)} />
        ) : (
          <textarea value={giaTri} onChange={(e) => doi(e.target.value)} />
        )}
        {canhBao && <p className="loi" style={{ margin: '0.3rem 0 0' }}>{canhBao}</p>}
      </div>
    </>
  );
}

function doanVan(s: string): string[] {
  return s.split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean);
}

function dong(s: string): string[] {
  return s.split('\n').map((p) => p.trim()).filter(Boolean);
}
