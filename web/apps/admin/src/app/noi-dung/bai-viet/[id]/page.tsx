'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { use, useEffect, useState } from 'react';
import type { AdminPostDetail, AdminTag, TranslationStatus } from '@travel/api-client';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { LOCALES, TRANG_THAI_BAN_DICH, suaLanCuoi, type LocaleMa } from '@/lib/noiDung';

/**
 * Sửa một bài viết (docs/22 M13).
 *
 * Ba khối: phần chung, thẻ, và bản dịch từng ngôn ngữ.
 *
 * **Thẻ là một khối riêng, không nằm trong khối chung.** Đó không phải chuyện
 * bố cục: endpoint của nó cũng riêng, vì bộ sinh mã dựng trường mảng vắng mặt
 * thành danh sách rỗng — gộp vào `PATCH` thì đổi mỗi cái ảnh bìa cũng lặng lẽ
 * gỡ sạch thẻ của bài.
 */
export default function SuaBaiViet({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const [bai, setBai] = useState<AdminPostDetail | null>(null);
  const [the, setThe] = useState<AdminTag[]>([]);
  const [loi, setLoi] = useState('');

  const [phienBan, setPhienBan] = useState(0);
  const napLai = async () => setPhienBan((v) => v + 1);

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const [chiTiet, dsThe] = await Promise.all([
          adminApi().getAdminPost({ id }),
          adminApi().listTags(),
        ]);
        if (conHieuLuc) {
          setBai(chiTiet);
          setThe(dsThe);
        }
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
          <Link href="/noi-dung?loai=bai-viet">← Về danh sách</Link>
        </p>
      </main>
    );
  }

  if (!bai) {
    return (
      <main>
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="dang-tai" />
        ))}
      </main>
    );
  }

  const tieuDe = bai.translations.find((t) => t.isSource)?.title
    ?? bai.translations[0]?.title
    ?? '(chưa có tiêu đề)';

  return (
    <main>
      <p className="phu">
        <Link href="/noi-dung?loai=bai-viet">← Nội dung khác</Link>
      </p>

      <div className="dau-trang">
        <div>
          <h1>{tieuDe}</h1>
          <p className="phu">{suaLanCuoi(bai.lastModifiedAt, bai.lastModifiedBy)}</p>
        </div>
      </div>

      <KhoiChung bai={bai} napLai={napLai} />
      <KhoiThe bai={bai} the={the} napLai={napLai} />

      {LOCALES.map((locale) => (
        <BanDich key={locale} bai={bai} locale={locale} napLai={napLai} />
      ))}

      <XoaBai bai={bai} />
    </main>
  );
}

function KhoiChung({ bai, napLai }: { bai: AdminPostDetail; napLai: () => Promise<void> }) {
  const [anh, setAnh] = useState(bai.heroImage ?? '');
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  async function luu() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().updatePost({
        id: bai.id,
        adminPostPatch: { ...(anh.trim() === '' ? {} : { heroImage: anh.trim() }) },
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
      <h2>Chung</h2>
      {loi && <p className="loi">{loi}</p>}
      {xong && !loi && <p className="xong">Đã lưu.</p>}

      <div className="loc">
        <div>
          <label htmlFor="anh">Ảnh đầu bài</label>
          <input
            id="anh"
            value={anh}
            placeholder="/img/blog/…"
            onChange={(e) => setAnh(e.target.value)}
            style={{ width: '100%', maxWidth: '28rem' }}
          />
        </div>
        <div className="hang">
          <button type="button" disabled={dangLuu} onClick={() => void luu()}>
            {dangLuu ? 'Đang lưu…' : 'Lưu'}
          </button>
        </div>
      </div>
    </section>
  );
}

/**
 * Thẻ — thay <b>toàn bộ</b> tập mỗi lần lưu.
 *
 * Tick vài ô rồi bấm Lưu, đúng như vai trò người dùng: một endpoint thêm cộng
 * một endpoint xoá nghĩa là bỏ tick rồi bấm Lưu sẽ không gỡ được gì.
 */
function KhoiThe({
  bai,
  the,
  napLai,
}: {
  bai: AdminPostDetail;
  the: AdminTag[];
  napLai: () => Promise<void>;
}) {
  const [chon, setChon] = useState<string[]>(bai.tags.map((t) => t.id));
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  async function luu() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().setPostTags({
        id: bai.id,
        adminPostTagAssignment: { tagIds: chon },
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
      <h2>Thẻ</h2>
      {loi && <p className="loi">{loi}</p>}
      {xong && !loi && <p className="xong">Đã lưu.</p>}

      {the.length === 0 ? (
        <p className="trong">Chưa có thẻ nào trong hệ thống.</p>
      ) : (
        <div className="loc">
          {the.map((t) => (
            <label key={t.id} className="hang" style={{ gap: '0.5rem' }}>
              <input
                type="checkbox"
                checked={chon.includes(t.id)}
                onChange={(e) =>
                  setChon((cu) =>
                    e.target.checked ? [...cu, t.id] : cu.filter((x) => x !== t.id),
                  )
                }
              />
              {t.name} <span className="phu">({t.code})</span>
            </label>
          ))}
          <div className="hang">
            <button type="button" disabled={dangLuu} onClick={() => void luu()}>
              {dangLuu ? 'Đang lưu…' : 'Lưu thẻ'}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

function BanDich({
  bai,
  locale,
  napLai,
}: {
  bai: AdminPostDetail;
  locale: LocaleMa;
  napLai: () => Promise<void>;
}) {
  const hienCo = bai.translations.find((t) => t.locale === locale);

  const [slug, setSlug] = useState(hienCo?.slug ?? '');
  const [tieuDe, setTieuDe] = useState(hienCo?.title ?? '');
  const [tomTat, setTomTat] = useState(hienCo?.excerpt ?? '');
  // Thân bài là MẢNG đoạn văn ở hợp đồng; ô nhập là một khối, và ranh giới đoạn
  // là dòng trống — quy ước quen thuộc, và nó giữ được đúng cấu trúc mảng.
  const [than, setThan] = useState((hienCo?.body ?? []).join('\n\n'));
  const [trangThai, setTrangThai] = useState<string>(hienCo?.status ?? 'DRAFT');
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  const slugSai = slug !== '' && !/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug);
  const doan = than.split(/\n{2,}/).map((d) => d.trim()).filter((d) => d !== '');

  async function luu() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().savePostTranslation({
        id: bai.id,
        locale,
        adminPostTranslationInput: {
          slug,
          title: tieuDe,
          excerpt: tomTat,
          body: doan,
          status: trangThai as TranslationStatus,
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
          <label htmlFor={`slug-${locale}`}>Slug</label>
          <input
            id={`slug-${locale}`}
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
            style={{ width: '100%', maxWidth: '28rem' }}
          />
          {slugSai && (
            <p className="loi">Slug chỉ dùng chữ thường không dấu, số và dấu gạch nối.</p>
          )}
        </div>

        <div>
          <label htmlFor={`tieu-de-${locale}`}>Tiêu đề</label>
          <input
            id={`tieu-de-${locale}`}
            value={tieuDe}
            onChange={(e) => setTieuDe(e.target.value)}
            style={{ width: '100%', maxWidth: '36rem' }}
          />
        </div>

        <div>
          <label htmlFor={`tom-tat-${locale}`}>Mô tả ngắn</label>
          <textarea
            id={`tom-tat-${locale}`}
            rows={2}
            value={tomTat}
            onChange={(e) => setTomTat(e.target.value)}
            style={{ width: '100%', maxWidth: '40rem' }}
          />
        </div>

        <div>
          <label htmlFor={`than-${locale}`}>Thân bài</label>
          <textarea
            id={`than-${locale}`}
            rows={12}
            value={than}
            onChange={(e) => setThan(e.target.value)}
            style={{ width: '100%', maxWidth: '48rem' }}
          />
          <p className="phu">
            Cách đoạn bằng một dòng trống. Hiện có {doan.length} đoạn. Không nhận HTML —
            nội dung đi thẳng ra trang, và HTML tự do là một lỗ chèn mã chờ sẵn.
          </p>
        </div>

        <div>
          <label htmlFor={`trang-thai-${locale}`}>Trạng thái</label>
          <select
            id={`trang-thai-${locale}`}
            value={trangThai}
            onChange={(e) => setTrangThai(e.target.value)}
          >
            {TRANG_THAI_BAN_DICH.map(([ma, ten]) => (
              <option key={ma} value={ma}>
                {ten}
              </option>
            ))}
          </select>
        </div>

        <div className="hang">
          <button
            type="button"
            disabled={dangLuu || slugSai || slug === '' || tieuDe === '' || doan.length === 0}
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
 * Xoá bài viết — **xoá mềm**, và ô xác nhận nói rõ hậu quả (`docs/22` mục 7).
 */
function XoaBai({ bai }: { bai: AdminPostDetail }) {
  const router = useRouter();
  const [hoi, setHoi] = useState(false);
  const [dangXoa, setDangXoa] = useState(false);
  const [loi, setLoi] = useState('');

  async function xoa() {
    setDangXoa(true);
    setLoi('');
    try {
      await adminApi().deletePost({ id: bai.id });
      router.push('/noi-dung?loai=bai-viet');
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
      setDangXoa(false);
    }
  }

  return (
    <section>
      <h2>Xoá</h2>
      {loi && <p className="loi">{loi}</p>}

      {!hoi ? (
        <button type="button" onClick={() => setHoi(true)}>
          Xoá bài viết
        </button>
      ) : (
        <div className="loc">
          <p>
            <strong>
              Gỡ bài này khỏi website ở cả hai ngôn ngữ. Bản ghi vẫn nằm lại trong cơ sở dữ
              liệu (xoá mềm), nhưng không có màn hình nào khôi phục lại được.
            </strong>
          </p>
          <div className="hang">
            <button type="button" disabled={dangXoa} onClick={() => void xoa()}>
              {dangXoa ? 'Đang xoá…' : 'Xoá thật'}
            </button>
            <button type="button" className="phu" disabled={dangXoa} onClick={() => setHoi(false)}>
              Thôi
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
