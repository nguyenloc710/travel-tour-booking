'use client';

import Link from 'next/link';
import { use, useEffect, useState } from 'react';
import type { AdminLectureDetail } from '@travel/api-client';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { LOCALES, ngay, suaLanCuoi, type LocaleMa } from '@/lib/noiDung';

/**
 * Sửa một buổi thuyết trình (docs/22 M13).
 *
 * **Thị trường chỉ đọc.** Buổi ở København thuộc `DK` dù nói bằng tiếng gì; đổi
 * thị trường của một buổi đã có người đăng ký là chuyện khác hẳn một lần sửa, và
 * cách đúng là tạo buổi mới.
 */
export default function SuaSuKien({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const [sk, setSk] = useState<AdminLectureDetail | null>(null);
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
        const kq = await adminApi().getAdminLecture({ id });
        if (conHieuLuc) setSk(kq);
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
          <Link href="/noi-dung?loai=su-kien">← Về danh sách</Link>
        </p>
      </main>
    );
  }

  if (!sk) {
    return (
      <main>
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="dang-tai" />
        ))}
      </main>
    );
  }

  const tieuDe = sk.translations.find((t) => t.isSource)?.title
    ?? sk.translations[0]?.title
    ?? '(chưa có tiêu đề)';

  return (
    <main>
      <p className="phu">
        <Link href="/noi-dung?loai=su-kien">← Nội dung khác</Link>
      </p>

      <div className="dau-trang">
        <div>
          <h1>{tieuDe}</h1>
          <p className="phu">
            {ngay(sk.eventDate)} · {sk.city} · thị trường {sk.market} ·{' '}
            {suaLanCuoi(sk.lastModifiedAt, sk.lastModifiedBy)}
          </p>
        </div>
      </div>

      <KhoiChung sk={sk} napLai={napLai} />

      {LOCALES.map((locale) => (
        <BanDich key={locale} sk={sk} locale={locale} napLai={napLai} />
      ))}
    </main>
  );
}

function KhoiChung({ sk, napLai }: { sk: AdminLectureDetail; napLai: () => Promise<void> }) {
  const [ngayTo, setNgayTo] = useState(iso(sk.eventDate));
  const [gio, setGio] = useState(sk.startTime ?? '');
  const [thanhPho, setThanhPho] = useState(sk.city);
  const [diaDiem, setDiaDiem] = useState(sk.venue ?? '');
  const [cho, setCho] = useState(String(sk.seats));
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  // Soi lại luật ở backend để không ai phải gặp 409 mới biết. Backend vẫn kiểm:
  // số đã đăng ký đổi được giữa lúc mở màn hình và lúc bấm Lưu.
  const duoiSoDaDangKy = Number(cho) < sk.seatsTaken;

  async function luu() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().updateLecture({
        id: sk.id,
        adminLecturePatch: {
          eventDate: new Date(ngayTo),
          ...(gio.trim() === '' ? {} : { startTime: gio.trim() }),
          city: thanhPho,
          ...(diaDiem.trim() === '' ? {} : { venue: diaDiem.trim() }),
          seats: Number(cho),
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
      <h2>Buổi</h2>
      {loi && <p className="loi">{loi}</p>}
      {xong && !loi && <p className="xong">Đã lưu.</p>}

      <div className="loc">
        <div className="hang">
          <div>
            <label htmlFor="ngay">Ngày</label>
            <input
              id="ngay"
              type="date"
              value={ngayTo}
              onChange={(e) => setNgayTo(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="gio">Giờ bắt đầu</label>
            <input
              id="gio"
              type="time"
              value={gio}
              onChange={(e) => setGio(e.target.value)}
            />
          </div>
        </div>

        <div>
          <label htmlFor="thanh-pho">Thành phố</label>
          <input
            id="thanh-pho"
            value={thanhPho}
            onChange={(e) => setThanhPho(e.target.value)}
            style={{ width: '100%', maxWidth: '24rem' }}
          />
        </div>

        <div>
          <label htmlFor="dia-diem">Địa điểm</label>
          <input
            id="dia-diem"
            value={diaDiem}
            onChange={(e) => setDiaDiem(e.target.value)}
            style={{ width: '100%', maxWidth: '28rem' }}
          />
        </div>

        <div>
          <label htmlFor="cho">Sức chứa</label>
          <input
            id="cho"
            type="number"
            min={1}
            value={cho}
            style={{ width: '7rem' }}
            onChange={(e) => setCho(e.target.value)}
          />
          <p className="phu">Đã đăng ký {sk.seatsTaken} chỗ.</p>
          {duoiSoDaDangKy && (
            <p className="loi">
              Không hạ được xuống dưới {sk.seatsTaken} — đó là số chỗ đã hứa cho người đã
              đăng ký.
            </p>
          )}
        </div>

        <div className="hang">
          <button
            type="button"
            disabled={dangLuu || duoiSoDaDangKy || thanhPho === ''}
            onClick={() => void luu()}
          >
            {dangLuu ? 'Đang lưu…' : 'Lưu'}
          </button>
        </div>
      </div>
    </section>
  );
}

function BanDich({
  sk,
  locale,
  napLai,
}: {
  sk: AdminLectureDetail;
  locale: LocaleMa;
  napLai: () => Promise<void>;
}) {
  const hienCo = sk.translations.find((t) => t.locale === locale);

  const [tieuDe, setTieuDe] = useState(hienCo?.title ?? '');
  const [moTa, setMoTa] = useState(hienCo?.description ?? '');
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState(false);

  async function luu() {
    setDangLuu(true);
    setLoi('');
    setXong(false);
    try {
      await adminApi().saveLectureTranslation({
        id: sk.id,
        locale,
        adminLectureTranslationInput: { title: tieuDe, description: moTa },
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
          <label htmlFor={`tieu-de-${locale}`}>Tiêu đề</label>
          <input
            id={`tieu-de-${locale}`}
            value={tieuDe}
            onChange={(e) => setTieuDe(e.target.value)}
            style={{ width: '100%', maxWidth: '36rem' }}
          />
        </div>

        <div>
          <label htmlFor={`mo-ta-${locale}`}>Mô tả</label>
          <textarea
            id={`mo-ta-${locale}`}
            rows={4}
            value={moTa}
            onChange={(e) => setMoTa(e.target.value)}
            style={{ width: '100%', maxWidth: '40rem' }}
          />
        </div>

        <div className="hang">
          <button
            type="button"
            disabled={dangLuu || tieuDe === '' || moTa === ''}
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
 * `Date` → `yyyy-mm-dd` cho `<input type="date">`.
 *
 * Đọc theo giờ **địa phương**: client sinh từ spec dựng `format: date` thành nửa
 * đêm địa phương, nên `toISOString()` ở phía đông UTC sẽ lùi một ngày.
 */
function iso(d: Date): string {
  const hai = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${hai(d.getMonth() + 1)}-${hai(d.getDate())}`;
}
