'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useRef, useState } from 'react';
import { t, type Locale, type Market } from '@travel/i18n';
import { formatMoney } from '@travel/ui';
import type { Booking, Departure, PriceBreakdown } from '@travel/api-client';
import { bookingApi, requestScope } from '@/lib/api';
import { duongDanDatTour, duongDanXacNhan } from '@/lib/routes';

export type TrangThai = {
  buoc: 1 | 2 | 3 | 4;
  departureId?: string;
  holdId?: string;
  hetHan?: string;
  pax: { paxTypeCode: string; count: number }[];
  singleTravellers?: number;
};

type HanhKhach = { paxTypeCode: string; fullName: string };

/**
 * Bốn bước đặt tour (docs/23 mục 2).
 *
 * **Bốn bước là bốn URL**, không phải bốn tab ẩn hiện bằng JavaScript: khách bấm
 * Back là chuyện chắc chắn xảy ra, và Back mất hết dữ liệu là cách nhanh nhất để
 * mất một đơn. Trạng thái ba bước đầu nằm trong tham số truy vấn.
 *
 * **Dữ liệu hành khách KHÔNG vào URL.** Tên là dữ liệu cá nhân, còn URL thì đi
 * thẳng vào nhật ký máy chủ (docs/31 mục 7). Bước 4 giữ chúng trong bộ nhớ trang
 * và gửi thẳng lên API.
 *
 * **Frontend không tự cộng trừ tiền.** Mỗi lần đổi lựa chọn là một lần gọi lại
 * endpoint tính giá — thứ tự cộng dồn tám bước và quy tắc làm tròn nằm ở docs/14,
 * và khi frontend tính khác backend thì con số ở bước 3 lệch con số ở bước 4:
 * khách thấy giá nhảy đúng vào lúc chuẩn bị trả tiền (docs/23 mục 2.2).
 */
export function DatTour({
  locale,
  market,
  slug,
  tieuDe,
  ngayKhoiHanh,
  trangThai,
}: {
  locale: Locale;
  market: Market;
  slug: string;
  tieuDe: string;
  ngayKhoiHanh: Departure[];
  trangThai: TrangThai;
}) {
  const router = useRouter();
  const phamVi = useMemo(() => requestScope(market, locale), [market, locale]);

  const [gia, setGia] = useState<PriceBreakdown | null>(null);
  const [loi, setLoi] = useState('');
  const [dangGui, setDangGui] = useState(false);

  /*
   * Khoá idempotency sinh MỘT LẦN cho một lần đặt, không sinh lại ở mỗi lần
   * thử — `docs/13` mục 7 và `docs/23` mục 5.2 nói thẳng điều đó, và mã ở đây
   * từng làm ngược lại: gọi `crypto.randomUUID()` ngay trong hàm gửi.
   *
   * Sinh lại là mất TOÀN BỘ tác dụng của khoá, và mất đúng vào ba tình huống
   * nó sinh ra để chống: khách bấm nút hai lần, mạng di động gửi lại yêu cầu
   * sau khi máy chủ đã xử lý xong, và khách bấm Back rồi bấm lại. Mỗi lần như
   * thế là một đơn thừa mà không ai phát hiện cho tới khi khách gọi điện hỏi
   * vì sao bị trừ tiền hai lần.
   *
   * Một khoá cho đơn, và một khoá cho MỖI ngày khởi hành: chọn lại ngày khác
   * là một lần giữ chỗ khác nên phải khoá khác, còn thử lại đúng ngày đó thì
   * phải là cùng khoá.
   */
  const [khoaDon] = useState(() => crypto.randomUUID());
  const khoaGiuCho = useRef(new Map<string, string>());

  function khoaChoNgay(departureId: string): string {
    const bang = khoaGiuCho.current;
    let khoa = bang.get(departureId);
    if (khoa === undefined) {
      khoa = crypto.randomUUID();
      bang.set(departureId, khoa);
    }
    return khoa;
  }

  const ngay = ngayKhoiHanh.find((d) => d.id === trangThai.departureId);
  const tongKhach = trangThai.pax.reduce((tong, p) => tong + p.count, 0);
  const khoaPax = JSON.stringify(trangThai.pax);

  // Tính lại giá ở bước 2 trở đi. Endpoint xem trước KHÔNG có tác dụng phụ nào,
  // nên gọi lại bao nhiêu lần cũng được (docs/13 mục 9.2).
  useEffect(() => {
    if (trangThai.buoc < 2 || !trangThai.departureId || tongKhach === 0) {
      return;
    }
    let conHieuLuc = true;
    void (async () => {
      try {
        const kq = await bookingApi().xemTruocGia({
          ...phamVi,
          pricingRequest: {
            departureId: trangThai.departureId as string,
            pax: JSON.parse(khoaPax).filter((p: { count: number }) => p.count > 0),
            singleTravellers: trangThai.singleTravellers,
          },
        });
        if (conHieuLuc) {
          setGia(kq);
        }
      } catch (ex) {
        if (conHieuLuc) {
          setGia(null);
          setLoi(await cauLoi(locale, ex));
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [
    phamVi,
    locale,
    khoaPax,
    tongKhach,
    trangThai.buoc,
    trangThai.departureId,
    trangThai.singleTravellers,
  ]);

  function di(moi: Partial<TrangThai>) {
    router.push(duongDanDatTour(locale, slug, dungThamSo({ ...trangThai, ...moi })));
  }

  /** Giữ chỗ tạo NGAY khi chọn xong ngày, không phải ở bước 4 — docs/23 mục 3.1. */
  async function chonNgay(d: Departure) {
    setLoi('');
    try {
      const giu = await bookingApi().giuCho({
        ...phamVi,
        idempotencyKey: khoaChoNgay(d.id),
        seatHoldRequest: { departureId: d.id, seats: Math.max(tongKhach, 1) },
      });
      di({ buoc: 2, departureId: d.id, holdId: giu.id, hetHan: giu.expiresAt.toISOString() });
    } catch (ex) {
      setLoi(await cauLoi(locale, ex));
    }
  }

  async function guiDon(hanhKhach: HanhKhach[], email: string, dienThoai: string) {
    setLoi('');
    setDangGui(true);
    try {
      const don: Booking = await bookingApi().datTour({
        ...phamVi,
        idempotencyKey: khoaDon,
        bookingRequest: {
          departureId: trangThai.departureId as string,
          seatHoldId: trangThai.holdId,
          pax: trangThai.pax.filter((p) => p.count > 0),
          singleTravellers: trangThai.singleTravellers,
          passengers: hanhKhach.map((h) => ({
            paxTypeCode: h.paxTypeCode,
            fullName: h.fullName,
          })),
          contactEmail: email,
          contactPhone: dienThoai,
        },
      });
      router.replace(duongDanXacNhan(locale, don.reference, email));
    } catch (ex) {
      setLoi(await cauLoi(locale, ex));
      setDangGui(false);
    }
  }

  return (
    <section className="dat">
      <h1>{tieuDe}</h1>
      <p className="phu">
        {t(locale, 'booking.step', { n: String(trangThai.buoc) })} ·{' '}
        {t(locale, `booking.step${trangThai.buoc}`)}
      </p>

      {trangThai.hetHan && trangThai.buoc > 1 && (
        <GiuChoConLai locale={locale} hetHan={trangThai.hetHan} quayLai={() => di({ buoc: 1 })} />
      )}

      {loi && (
        <p className="state state--error" role="alert">
          {loi}
        </p>
      )}

      {/* `dangChon` là ngày khách đã bấm ở bảng của trang chi tiết. Bước 1 làm
          nổi nó lên để khách thấy lựa chọn của mình được mang sang, nhưng VẪN
          phải bấm một lần nữa: giữ chỗ là một lần ghi, và tự ghi khi trang vừa
          mở nghĩa là Next nạp trước lúc rê chuột cũng giữ chỗ. */}
      {trangThai.buoc === 1 && (
        <Buoc1
          locale={locale}
          ngayKhoiHanh={ngayKhoiHanh}
          chon={chonNgay}
          dangChon={trangThai.departureId}
        />
      )}
      {trangThai.buoc === 2 && <Buoc2 locale={locale} trangThai={trangThai} doi={di} />}
      {trangThai.buoc === 3 && (
        <Buoc3 locale={locale} trangThai={trangThai} doi={di} tongKhach={tongKhach} />
      )}
      {trangThai.buoc === 4 && (
        <Buoc4
          locale={locale}
          trangThai={trangThai}
          dangGui={dangGui}
          gui={guiDon}
          quayLai={() => di({ buoc: 3 })}
        />
      )}

      {trangThai.buoc > 1 && <PhanRaGia locale={locale} gia={gia} ngay={ngay} />}
    </section>
  );
}

/* ------------------------------------------------------------ bước 1 */

function Buoc1({
  locale,
  ngayKhoiHanh,
  chon,
  dangChon,
}: {
  locale: Locale;
  ngayKhoiHanh: Departure[];
  chon: (d: Departure) => void;
  dangChon?: string;
}) {
  // Ngày đã đóng bán thì không chọn được — kiểm ở frontend để khách khỏi mất
  // công; backend vẫn kiểm lại bằng DEPARTURE_CLOSED và DEPARTURE_SOLD_OUT
  // (docs/23 mục 2.1).
  const moBan = ngayKhoiHanh.filter((d) => d.status !== 'SOLD_OUT' && d.seatsAvailable > 0);

  if (moBan.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'booking.noDepartures')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  const daChon = dangChon !== undefined && moBan.some((d) => d.id === dangChon);

  return (
    <>
      {daChon && <p className="dat-da-chon">{t(locale, 'booking.preselected')}</p>}

      <ul className="dat-ngay">
        {moBan.map((d) => (
          <li key={d.id}>
            <button
              type="button"
              onClick={() => chon(d)}
              aria-current={d.id === dangChon ? 'true' : undefined}
            >
              <strong>{ngayDai(d.departDate, locale)}</strong>
              <span>
                {/* Số ít và số nhiều là hai khoá riêng: tiếng Đan viết "1 plads"
                    chứ không "1 pladser". Tiếng Việt không phân biệt nên hai khoá
                    trùng nội dung — vẫn giữ đủ hai, để bộ kiểm độ phủ so được
                    từng khoá một. */}
                {t(
                  locale,
                  d.seatsAvailable === 1 ? 'booking.seatsLeft.one' : 'booking.seatsLeft.many',
                  { count: String(d.seatsAvailable) },
                )}
                {d.departureCity ? ` · ${d.departureCity}` : ''}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}

/* ------------------------------------------------------------ bước 2 */

function Buoc2({
  locale,
  trangThai,
  doi,
}: {
  locale: Locale;
  trangThai: TrangThai;
  doi: (moi: Partial<TrangThai>) => void;
}) {
  const [pax, setPax] = useState(trangThai.pax);
  const tong = pax.reduce((t2, p) => t2 + p.count, 0);

  return (
    <>
      <fieldset className="dat-o">
        <legend>{t(locale, 'booking.step2')}</legend>
        {pax.map((p, i) => (
          <div key={p.paxTypeCode} className="dat-hang">
            <label htmlFor={`pax-${p.paxTypeCode}`}>{p.paxTypeCode}</label>
            <input
              id={`pax-${p.paxTypeCode}`}
              type="number"
              min={0}
              max={20}
              value={p.count}
              onChange={(e) =>
                setPax(pax.map((x, j) => (i === j ? { ...x, count: Number(e.target.value) } : x)))
              }
            />
          </div>
        ))}
      </fieldset>

      <div className="dat-nut">
        <button type="button" className="phu" onClick={() => doi({ buoc: 1 })}>
          {t(locale, 'booking.back')}
        </button>
        <button type="button" disabled={tong === 0} onClick={() => doi({ buoc: 3, pax })}>
          {t(locale, 'booking.next')}
        </button>
      </div>
    </>
  );
}

/* ------------------------------------------------------------ bước 3 */

function Buoc3({
  locale,
  trangThai,
  doi,
  tongKhach,
}: {
  locale: Locale;
  trangThai: TrangThai;
  doi: (moi: Partial<TrangThai>) => void;
  tongKhach: number;
}) {
  const [motMinh, setMotMinh] = useState(trangThai.singleTravellers ?? 0);

  return (
    <>
      <fieldset className="dat-o">
        <legend>{t(locale, 'booking.step3')}</legend>
        <div className="dat-hang">
          <label htmlFor="mot-minh">{t(locale, 'booking.singleRooms')}</label>
          <input
            id="mot-minh"
            type="number"
            min={0}
            max={tongKhach}
            value={motMinh}
            onChange={(e) => setMotMinh(Number(e.target.value))}
          />
        </div>
        <p className="phu">{t(locale, 'booking.singleRooms.help')}</p>
      </fieldset>

      <div className="dat-nut">
        <button type="button" className="phu" onClick={() => doi({ buoc: 2 })}>
          {t(locale, 'booking.back')}
        </button>
        <button type="button" onClick={() => doi({ buoc: 4, singleTravellers: motMinh })}>
          {t(locale, 'booking.next')}
        </button>
      </div>
    </>
  );
}

/* ------------------------------------------------------------ bước 4 */

function Buoc4({
  locale,
  trangThai,
  dangGui,
  gui,
  quayLai,
}: {
  locale: Locale;
  trangThai: TrangThai;
  dangGui: boolean;
  gui: (hanhKhach: HanhKhach[], email: string, dienThoai: string) => void;
  quayLai: () => void;
}) {
  // Một ô tên cho mỗi khách, dựng từ số khách đã chọn ở bước 2.
  const [hanhKhach, setHanhKhach] = useState<HanhKhach[]>(() =>
    trangThai.pax.flatMap((p) =>
      Array.from({ length: p.count }, () => ({ paxTypeCode: p.paxTypeCode, fullName: '' })),
    ),
  );
  const [email, setEmail] = useState('');
  const [dienThoai, setDienThoai] = useState('');

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        gui(hanhKhach, email, dienThoai);
      }}
    >
      <fieldset className="dat-o">
        <legend>{t(locale, 'booking.passengers')}</legend>
        {hanhKhach.map((h, i) => (
          <div key={i} className="dat-hang">
            <label htmlFor={`hk-${i}`}>
              {h.paxTypeCode} {i + 1}
            </label>
            <input
              id={`hk-${i}`}
              required
              autoComplete="off"
              placeholder={t(locale, 'booking.fullName')}
              value={h.fullName}
              onChange={(e) =>
                setHanhKhach(
                  hanhKhach.map((x, j) => (i === j ? { ...x, fullName: e.target.value } : x)),
                )
              }
            />
          </div>
        ))}
      </fieldset>

      <fieldset className="dat-o">
        <legend>{t(locale, 'booking.contact')}</legend>
        <div className="dat-hang">
          <label htmlFor="email">{t(locale, 'booking.email')}</label>
          <input
            id="email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div className="dat-hang">
          <label htmlFor="dt">{t(locale, 'booking.phone')}</label>
          <input
            id="dt"
            type="tel"
            required
            autoComplete="tel"
            value={dienThoai}
            onChange={(e) => setDienThoai(e.target.value)}
          />
        </div>
      </fieldset>

      <div className="dat-nut">
        <button type="button" className="phu" onClick={quayLai}>
          {t(locale, 'booking.back')}
        </button>
        <button type="submit" disabled={dangGui}>
          {dangGui ? t(locale, 'booking.submitting') : t(locale, 'booking.submit')}
        </button>
      </div>
    </form>
  );
}

/* ------------------------------------------------------------ giữ chỗ */

/**
 * Một dòng **chữ** nói còn bao nhiêu thời gian, không phải đồng hồ đếm ngược
 * nhấp nháy: nhóm khách này phản ứng tệ với sức ép, và một cái đồng hồ chạy giật
 * làm họ bỏ ngang (docs/23 mục 3.2).
 *
 * Còn dưới 5 phút thì nhắc thêm một câu, kèm cách gia hạn — quay lại bước 1 và
 * chọn lại ngày đó.
 */
function GiuChoConLai({
  locale,
  hetHan,
  quayLai,
}: {
  locale: Locale;
  hetHan: string;
  quayLai: () => void;
}) {
  const [conLai, setConLai] = useState(() => Date.parse(hetHan) - Date.now());

  useEffect(() => {
    // Nhịp 30 giây, không phải mỗi giây: dòng chữ chỉ hiện GIỜ kết thúc, nên cập
    // nhật dày hơn không thêm thông tin nào mà chỉ làm trang nhấp nháy.
    const h = setInterval(() => setConLai(Date.parse(hetHan) - Date.now()), 30_000);
    return () => clearInterval(h);
  }, [hetHan]);

  if (conLai <= 0) {
    return (
      <p className="state state--error" role="alert">
        {t(locale, 'booking.hold.expired')}{' '}
        <button type="button" className="phu" onClick={quayLai}>
          {t(locale, 'booking.step1')}
        </button>
      </p>
    );
  }

  const gio = new Intl.DateTimeFormat(locale === 'da' ? 'da-DK' : 'vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(hetHan));

  return (
    <p className="dat-giu">
      {t(locale, 'booking.hold.until', { time: gio })}
      {conLai < 5 * 60_000 && <> {t(locale, 'booking.hold.soon')}</>}
    </p>
  );
}

/* ------------------------------------------------------------ bảng giá */

/**
 * Bảng phân rã giá — hiện từ bước 2 trở đi, và **luôn là giá backend tính**.
 *
 * Dòng bằng 0 không xuất hiện; backend đã bỏ sẵn (docs/14 mục 2.2). `amount` là
 * chuỗi, chỉ định dạng, không `parseFloat`.
 */
function PhanRaGia({
  locale,
  gia,
  ngay,
}: {
  locale: Locale;
  gia: PriceBreakdown | null;
  ngay: Departure | undefined;
}) {
  if (!gia) {
    return <div className="skeleton-bar" />;
  }

  return (
    <div className="dat-gia">
      <h2>{t(locale, 'booking.priceHeading')}</h2>
      {ngay && <p className="phu">{ngayDai(ngay.departDate, locale)}</p>}

      <table>
        <tbody>
          {gia.lines.map((d, i) => (
            <tr key={i}>
              <th scope="row">
                {t(locale, `priceLine.${d.kind}`)}
                {d.quantity && <span className="phu"> × {d.quantity}</span>}
              </th>
              <td>{formatMoney(d.amount, locale)}</td>
            </tr>
          ))}
          <tr className="dat-gia__tong">
            <th scope="row">{t(locale, 'booking.total')}</th>
            <td>{formatMoney(gia.total, locale)}</td>
          </tr>
          <tr>
            <th scope="row">{t(locale, 'booking.deposit')}</th>
            <td>{formatMoney(gia.deposit, locale)}</td>
          </tr>
          <tr>
            <th scope="row">{t(locale, 'booking.balance')}</th>
            <td>{formatMoney(gia.balance, locale)}</td>
          </tr>
        </tbody>
      </table>

      <p className="price-from__disclaimer">{t(locale, 'price.disclaimer')}</p>
    </div>
  );
}

/* ------------------------------------------------------------ tiện ích */

/**
 * Trạng thái ba bước đầu sang tham số truy vấn.
 *
 * Không có tên hành khách, không có email, không có điện thoại — xem chú thích
 * của component chính.
 */
function dungThamSo(tt: TrangThai): URLSearchParams {
  const q = new URLSearchParams({ buoc: String(tt.buoc) });
  if (tt.departureId) {
    q.set('ngay', tt.departureId);
  }
  if (tt.holdId) {
    q.set('giu', tt.holdId);
  }
  if (tt.hetHan) {
    q.set('het', tt.hetHan);
  }
  const khach = tt.pax.filter((p) => p.count > 0).map((p) => `${p.paxTypeCode}:${p.count}`);
  if (khach.length > 0) {
    q.set('khach', khach.join(','));
  }
  if (tt.singleTravellers) {
    q.set('mot', String(tt.singleTravellers));
  }
  return q;
}

function ngayDai(d: Date, locale: Locale): string {
  return new Intl.DateTimeFormat(locale === 'da' ? 'da-DK' : 'vi-VN', {
    dateStyle: 'long',
  }).format(d);
}

/**
 * Mã lỗi của API sang câu tiếng người của locale đang xem — docs/13 mục 5.
 *
 * Mã chưa có bản dịch thì hiện câu chung, **không hiện mã ra khách**
 * (web/CLAUDE.md mục 4).
 */
async function cauLoi(locale: Locale, loi: unknown): Promise<string> {
  let ma = '';
  if (loi && typeof loi === 'object' && 'response' in loi) {
    try {
      const than = await (loi as { response: Response }).response.clone().json();
      ma = String(than.code ?? '');
    } catch {
      ma = '';
    }
  }

  const khoa = `error.${ma}`;
  const cau = t(locale, khoa);
  return ma !== '' && cau !== khoa ? cau : t(locale, 'error.generic');
}
