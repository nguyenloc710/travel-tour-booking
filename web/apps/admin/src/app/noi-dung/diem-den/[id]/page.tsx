'use client';

import Link from 'next/link';
import { use, useCallback, useEffect, useState } from 'react';
import type { AdminDestinationDetail } from '@travel/api-client';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { LOCALES, suaLanCuoi, type LocaleMa } from '@/lib/noiDung';
import { MediaList } from '@/components/MediaList';
import { MediaUploadUrlRequestFolderEnum } from '@travel/api-client';

/**
 * Sửa một điểm đến (docs/22 M13).
 *
 * Hai khối: thứ tự sắp xếp, và bản dịch từng ngôn ngữ.
 *
 * **Mã (`code`) chỉ đọc.** Nó là định danh nội bộ mà dữ liệu mồi, tài liệu và
 * câu chuyện của cả đội đang trỏ tới — đổi mã là đổi thứ người ta gọi tên nhau
 * bằng, và không có màn hình nào cho việc đó.
 */
export default function SuaDiemDen({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const [dd, setDd] = useState<AdminDestinationDetail | null>(null);
  const [loi, setLoi] = useState('');

  // Bộ đếm phiên bản thay cho một hàm nạp trong danh sách phụ thuộc: hàm dựng
  // mới ở mỗi lần render, nên đưa nó vào `useEffect` là mời một vòng gọi API
  // không dừng.
  const [phienBan, setPhienBan] = useState(0);
  const napLai = async () => setPhienBan((v) => v + 1);

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const kq = await adminApi().getAdminDestination({ id });
        if (conHieuLuc) setDd(kq);
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(
            laKhongDuQuyen(ex)
              ? 'Vai trò của bạn không xem được nội dung này.'
              : await loiTiengViet(ex),
          );
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [id, phienBan]);

  if (loi) {
    return (
      <main>
        <p className="loi">{loi}</p>
        <p>
          <Link href="/noi-dung?loai=diem-den">← Về danh sách</Link>
        </p>
      </main>
    );
  }

  if (!dd) {
    return (
      <main>
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="dang-tai" />
        ))}
      </main>
    );
  }

  const tenNguon = dd.translations.find((t) => t.isSource)?.name ?? dd.code;

  return (
    <main>
      <p className="phu">
        <Link href="/noi-dung?loai=diem-den">← Nội dung khác</Link>
      </p>

      <div className="dau-trang">
        <div>
          <h1>{tenNguon}</h1>
          <p className="phu">
            <code>{dd.code}</code> · {dd.regionName} · {suaLanCuoi(dd.lastModifiedAt, dd.lastModifiedBy)}
          </p>
        </div>
      </div>

      {/* Số sản phẩm đang trỏ tới: nói TRƯỚC hậu quả, thay vì để người dùng bấm
          Xoá rồi nhận 409 mà không hiểu vì sao (docs/22 mục 7). */}
      {dd.productCount !== undefined && dd.productCount > 0 && (
        <p className="trong">
          {dd.productCount} sản phẩm đang trỏ tới điểm đến này, nên không xoá được. Xoá nó
          sẽ làm những sản phẩm đó biến mất khỏi website mà không có lỗi nào ghi ra.
        </p>
      )}

      <ThuTu dd={dd} napLai={napLai} />

      {/* `vi` chưa có bản dịch thì vẫn phải mở được ô nhập, nếu không thì không
          có đường tạo bản dịch đầu tiên. */}
      {LOCALES.map((locale) => (
        <BanDich key={locale} dd={dd} locale={locale} napLai={napLai} />
      ))}

      {/* Ảnh và video của điểm đến — chúng hiện ở khối "bản đồ lộ trình" của MỌI
          tour đi qua đây (docs/05 mục 6.1). Đó là lý do chúng thuộc điểm đến chứ
          không thuộc tour: cùng một Sa Pa xuất hiện trong nhiều chuyến. */}
      <MediaDiemDen id={dd.id} />
    </main>
  );
}

function ThuTu({
  dd,
  napLai,
}: {
  dd: AdminDestinationDetail;
  napLai: () => Promise<void>;
}) {
  const [thuTu, setThuTu] = useState(String(dd.sortOrder));
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  async function luu() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().updateDestination({
        id: dd.id,
        adminDestinationPatch: { sortOrder: Number(thuTu) },
      });
      await napLai();
      setXong(true);
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  return (
    <section>
      <h2>Sắp xếp</h2>
      {loi && <p className="loi">{loi}</p>}
      {xong && !loi && <p className="xong">Đã lưu.</p>}

      <div className="hang">
        <label htmlFor="thu-tu">Thứ tự hiện</label>
        <input
          id="thu-tu"
          type="number"
          min={0}
          max={999}
          value={thuTu}
          style={{ width: '6rem' }}
          onChange={(e) => setThuTu(e.target.value)}
        />
        <button type="button" disabled={dangLuu} onClick={() => void luu()}>
          {dangLuu ? 'Đang lưu…' : 'Lưu'}
        </button>
      </div>
    </section>
  );
}

/**
 * Một bản dịch.
 *
 * Ô nhập **không điền sẵn bằng bản nguồn** khi bản dịch còn trống: người dịch sẽ
 * sửa qua loa và để sót nguyên câu tiếng Đan (`docs/02` mục 8).
 *
 * Không ẩn ô của locale mình không sửa được: người dịch vẫn cần **đọc** bản
 * nguồn để dịch. Máy chủ mới là chỗ từ chối, và nó trả `403` — giao diện chỉ vô
 * hiệu hoá nút để không ai phải gặp lỗi đó.
 */
function BanDich({
  dd,
  locale,
  napLai,
}: {
  dd: AdminDestinationDetail;
  locale: LocaleMa;
  napLai: () => Promise<void>;
}) {
  const hienCo = dd.translations.find((t) => t.locale === locale);

  const [slug, setSlug] = useState(hienCo?.slug ?? '');
  const [ten, setTen] = useState(hienCo?.name ?? '');
  const [tomTat, setTomTat] = useState(hienCo?.summary ?? '');
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  // Slug có dấu thì CSDL từ chối bằng `ck_dt_slug`; báo ngay khi gõ thì đỡ mất
  // một lần gửi đi rồi nhận lỗi không nói gì.
  const slugSai = slug !== '' && !/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug);

  async function luu() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().saveDestinationTranslation({
        id: dd.id,
        locale,
        adminDestinationTranslationInput: {
          slug,
          name: ten,
          ...(tomTat.trim() === '' ? {} : { summary: tomTat }),
        },
      });
      await napLai();
      setXong(true);
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  return (
    <section>
      <h2>
        Bản {locale}
        {locale === 'da' && <span className="phu"> — ngôn ngữ nguồn</span>}
        {!hienCo && <span className="phu"> — chưa có</span>}
      </h2>

      {hienCo && <p className="phu">{suaLanCuoi(hienCo.lastModifiedAt, hienCo.lastModifiedBy)}</p>}
      {loi && <p className="loi">{loi}</p>}
      {xong && !loi && <p className="xong">Đã lưu.</p>}

      <div className="loc">
        <div>
          <label htmlFor={`slug-${locale}`}>Slug (đường dẫn)</label>
          <input
            id={`slug-${locale}`}
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
            style={{ width: '100%', maxWidth: '28rem' }}
          />
          {slugSai && (
            <p className="loi">
              Slug chỉ dùng chữ thường không dấu, số và dấu gạch nối: <code>ha-noi</code>,
              không phải <code>hà-nội</code>.
            </p>
          )}
        </div>

        <div>
          <label htmlFor={`ten-${locale}`}>Tên</label>
          <input
            id={`ten-${locale}`}
            value={ten}
            onChange={(e) => setTen(e.target.value)}
            style={{ width: '100%', maxWidth: '28rem' }}
          />
        </div>

        <div>
          <label htmlFor={`tom-tat-${locale}`}>Giới thiệu ngắn</label>
          <textarea
            id={`tom-tat-${locale}`}
            rows={3}
            value={tomTat}
            onChange={(e) => setTomTat(e.target.value)}
            style={{ width: '100%', maxWidth: '40rem' }}
          />
        </div>

        <div className="hang">
          <button
            type="button"
            disabled={dangLuu || slugSai || slug === '' || ten === ''}
            onClick={() => void luu()}
          >
            {dangLuu ? 'Đang lưu…' : `Lưu bản ${locale}`}
          </button>
        </div>
      </div>
    </section>
  );
}

/**
 * Danh sách ảnh và video của điểm đến.
 *
 * Bọc {@code MediaList} trong một component riêng để hai hàm truyền vào nó ổn
 * định qua các lần dựng lại — {@code useCallback} cần một chỗ để bám, và trang
 * này còn hai khối khác cũng dựng lại theo trạng thái của mình.
 */
function MediaDiemDen({ id }: { id: string }) {
  const nap = useCallback(
    async () => (await adminApi().getDestinationMedia({ id })).items,
    [id],
  );
  const luu = useCallback(
    async (assetIds: string[]) =>
      (await adminApi().saveDestinationMedia({ id, mediaOrder: { assetIds } })).items,
    [id],
  );

  return (
    <MediaList
      nap={nap}
      luu={luu}
      allowVideo
      folder={MediaUploadUrlRequestFolderEnum.DiemDen}
      tieuDe="Ảnh và video của điểm đến"
    />
  );
}
