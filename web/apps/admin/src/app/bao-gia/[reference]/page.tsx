'use client';

import Link from 'next/link';
import { use, useEffect, useState } from 'react';
import type { AdminQuoteDetail, QuoteLineInput } from '@travel/api-client';
import { formatMoney } from '@travel/ui';
import { adminApi, errorInfo, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import {
  KHOA_DONG_GIA,
  KHOA_DONG_GIA_MAC_DINH,
  conLai,
  hauQua,
  mauTrangThai,
  ngay,
  ngayGio,
  suaBangGiaDuoc,
  tenDongGia,
  tenTrangThai,
  thaoTacChoPhep,
} from '@/lib/baoGia';

/**
 * Chi tiết một báo giá (docs/22 M8).
 *
 * Ba khối: yêu cầu khách gửi, bảng giá đang dựng, và thao tác. Khối giữa là lý
 * do màn hình này tồn tại — giá của tour riêng **được phép khác** bảng giá theo
 * bậc (docs/14 mục 7 quy tắc 5), và đó chính là toàn bộ lý do `PRIVATE_TOUR`
 * tồn tại như một loại sản phẩm riêng.
 *
 * **Không có nút sửa yêu cầu của khách.** Những gì khách viết là dữ liệu khách
 * gửi; sửa nó là sửa lời người khác.
 */
export default function ChiTietBaoGia({ params }: { params: Promise<{ reference: string }> }) {
  const { reference } = use(params);

  const [baoGia, setBaoGia] = useState<AdminQuoteDetail | null>(null);
  const [loi, setLoi] = useState('');

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ket_qua = await adminApi().getQuote({ reference });
        if (conHieuLuc) {
          setBaoGia(ket_qua);
        }
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(
            laKhongDuQuyen(ex) ? 'Vai trò của bạn không xem được báo giá.' : await loiTiengViet(ex),
          );
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, [reference]);

  if (loi) {
    return (
      <main>
        <p className="loi">{loi}</p>
        <p>
          <Link href="/bao-gia">← Về danh sách báo giá</Link>
        </p>
      </main>
    );
  }

  if (!baoGia) {
    return (
      <main>
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="dang-tai" />
        ))}
      </main>
    );
  }

  const con = conLai(baoGia.validUntil);

  return (
    <main>
      <p className="phu">
        <Link href="/bao-gia">← Báo giá</Link>
      </p>

      <div className="dau-trang">
        <div>
          <h1>{baoGia.reference}</h1>
          <p className="phu">
            {baoGia.productTitle}
            {' · '}
            {baoGia.market} · {baoGia.locale}
            {' · nhận lúc '}
            {ngayGio(baoGia.createdAt)}
          </p>
        </div>
        <span
          className={`nhan ${mauTrangThai(baoGia.status)}`}
          style={{ marginLeft: 'auto', alignSelf: 'center' }}
        >
          {tenTrangThai(baoGia.status)}
        </span>
      </div>

      {/* Quá hạn mà job chưa quét tới thì trạng thái vẫn là "Đã gửi khách".
          Nói thẳng ra ở đây, vì thao tác "Khách đã nhận" sẽ bị máy chủ từ chối
          và không có gì trên màn hình giải thích vì sao (docs/14 mục 7 quy tắc 4). */}
      {baoGia.status === 'SENT' && con !== null && con < 0 && (
        <p className="loi">
          Báo giá này đã quá hạn ngày {ngay(baoGia.validUntil)}. Không ghi nhận khách đồng ý
          được nữa — báo giá <strong>không tự gia hạn</strong>; khách muốn tiếp thì gửi một
          yêu cầu mới.
        </p>
      )}

      <ThaoTac baoGia={baoGia} onXong={setBaoGia} />

      <section>
        <h2>Khách yêu cầu gì</h2>
        <table className="bang-doi">
          <tbody>
            <tr>
              <th scope="row">Tên</th>
              <td>{baoGia.contactName}</td>
            </tr>
            <tr>
              <th scope="row">Email</th>
              <td>{baoGia.contactEmail}</td>
            </tr>
            <tr>
              <th scope="row">Điện thoại</th>
              <td>{baoGia.contactPhone}</td>
            </tr>
            <tr>
              <th scope="row">Số người</th>
              <td>{baoGia.partySize}</td>
            </tr>
            <tr>
              <th scope="row">Ngày muốn đi</th>
              <td>
                {ngay(baoGia.requestedDate)}
                {baoGia.requestedDate === undefined && (
                  <span className="phu"> — khách chưa chốt ngày</span>
                )}
              </td>
            </tr>
            <tr>
              <th scope="row">Yêu cầu riêng</th>
              <td>{baoGia.message ?? <span className="phu">— không có —</span>}</td>
            </tr>
            <tr>
              <th scope="row">Ràng buộc của tour</th>
              <td>
                báo trước tối thiểu {baoGia.leadTimeDays} ngày · báo giá có hiệu lực{' '}
                {baoGia.quoteValidDays} ngày
              </td>
            </tr>
            <tr>
              <th scope="row">Hạn hiệu lực</th>
              <td>
                {baoGia.validUntil ? (
                  <>
                    {ngay(baoGia.validUntil)}{' '}
                    <span className="phu">
                      ({con !== null && con < 0 ? 'đã quá hạn' : `còn ${con} ngày`})
                    </span>
                  </>
                ) : (
                  <span className="phu">chưa gửi nên chưa có hạn</span>
                )}
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <BangGia baoGia={baoGia} onXong={setBaoGia} />
    </main>
  );
}

/* ------------------------------------------------------------ bảng giá */

type Dong = { labelKey: string; quantity: string; unitAmount: string; amount: string };

/**
 * Dựng bảng giá.
 *
 * **Tổng không có ô nhập.** Máy chủ cộng từ các dòng và trả lại; con số hiện ở
 * đây trước khi lưu chỉ là bản xem trước để tư vấn viên soát lại. Nhận tổng từ
 * người gõ là mở đường cho một báo giá mà tổng không khớp bảng — thứ khách mang
 * ra tranh cãi.
 *
 * Sau khi gửi thì bảng thành **chỉ đọc**: bảng giá lúc đó là thứ khách đang cầm
 * trong tay, và sửa nó sau lưng khách là thứ không giải thích được khi hai bên
 * mang hai bản ra đối chiếu.
 */
function BangGia({
  baoGia,
  onXong,
}: {
  baoGia: AdminQuoteDetail;
  onXong: (b: AdminQuoteDetail) => void;
}) {
  const suaDuoc = suaBangGiaDuoc(baoGia.status);

  const [dong, setDong] = useState<Dong[]>(() =>
    baoGia.lines.length > 0
      ? baoGia.lines.map((l) => ({
          labelKey: l.labelKey,
          quantity: l.quantity ?? '',
          unitAmount: l.unitAmount?.amount ?? '',
          amount: l.amount.amount,
        }))
      : [{ labelKey: KHOA_DONG_GIA_MAC_DINH, quantity: '', unitAmount: '', amount: '' }],
  );
  const [dangLuu, setDangLuu] = useState(false);
  const [loi, setLoi] = useState('');
  const [daLuu, setDaLuu] = useState(false);

  if (!suaDuoc) {
    return (
      <section>
        <h2>Bảng giá</h2>
        {baoGia.lines.length === 0 ? (
          <p className="trong">Báo giá này không có dòng nào.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Khoản</th>
                <th>Số lượng</th>
                <th>Đơn giá</th>
                <th>Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              {baoGia.lines.map((l) => (
                <tr key={l.seq}>
                  <td>{tenDongGia(l.labelKey)}</td>
                  <td>{l.quantity ?? '—'}</td>
                  <td>{l.unitAmount ? formatMoney(l.unitAmount, 'vi') : '—'}</td>
                  <td>{formatMoney(l.amount, 'vi')}</td>
                </tr>
              ))}
              <tr>
                <th colSpan={3} scope="row" style={{ textAlign: 'right' }}>
                  Tổng
                </th>
                <td>
                  <strong>{baoGia.total ? formatMoney(baoGia.total, 'vi') : '—'}</strong>
                </td>
              </tr>
            </tbody>
          </table>
        )}
        <p className="phu">
          Đã gửi cho khách nên bảng giá khoá lại — khách đang cầm đúng bản này.
        </p>
      </section>
    );
  }

  function doi(i: number, truong: keyof Dong, giaTri: string) {
    setDaLuu(false);
    setDong((cu) => cu.map((d, j) => (i === j ? { ...d, [truong]: giaTri } : d)));
  }

  async function luu() {
    setDangLuu(true);
    setLoi('');
    try {
      const gui: QuoteLineInput[] = dong
        .filter((d) => d.amount.trim() !== '')
        .map((d) => ({
          labelKey: d.labelKey,
          // Trường rỗng BỎ HẲN, không gửi chuỗi rỗng: spec đòi dạng số, và
          // chuỗi rỗng trượt qua đây thành `400` không nói được lý do.
          ...(d.quantity.trim() === '' ? {} : { quantity: d.quantity.trim() }),
          ...(d.unitAmount.trim() === '' ? {} : { unitAmount: d.unitAmount.trim() }),
          amount: d.amount.trim(),
        }));

      if (gui.length === 0) {
        setLoi('Cần ít nhất một dòng có thành tiền.');
        return;
      }

      const moi = await adminApi().saveQuoteLines({
        reference: baoGia.reference,
        adminQuoteLinesInput: {
          // Tiền tệ theo THỊ TRƯỜNG của báo giá, không phải một ô cho người
          // chọn: chọn được tiền tệ khác là mở đường cho một lần quy đổi tỷ
          // giá, và hệ thống này không có tỷ giá ở đâu cả.
          currency: tienTe(baoGia),
          lines: gui,
        },
      });
      onXong(moi);
      setDaLuu(true);
    } catch (ex) {
      setLoi(await loiTiengViet(ex));
    } finally {
      setDangLuu(false);
    }
  }

  return (
    <section>
      <h2>Bảng giá</h2>
      <p className="phu">
        Giá ở đây <strong>được phép khác</strong> bảng giá theo bậc — đó là lý do tour riêng
        đi qua bước báo giá. Dòng giảm trừ ghi số âm.
      </p>

      {loi && <p className="loi">{loi}</p>}
      {daLuu && !loi && <p className="phu">Đã lưu bảng giá.</p>}

      <table>
        <thead>
          <tr>
            <th style={{ width: '18rem' }}>Khoản</th>
            <th>Số lượng</th>
            <th>Đơn giá</th>
            <th>Thành tiền</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {dong.map((d, i) => (
            <tr key={i}>
              <td>
                <select value={d.labelKey} onChange={(e) => doi(i, 'labelKey', e.target.value)}>
                  {KHOA_DONG_GIA.map(([ma, ten]) => (
                    <option key={ma} value={ma}>
                      {ten}
                    </option>
                  ))}
                </select>
              </td>
              <td>
                <input
                  value={d.quantity}
                  inputMode="decimal"
                  placeholder="—"
                  style={{ width: '5rem' }}
                  onChange={(e) => doi(i, 'quantity', e.target.value)}
                />
              </td>
              <td>
                <input
                  value={d.unitAmount}
                  inputMode="decimal"
                  placeholder="—"
                  style={{ width: '8rem' }}
                  onChange={(e) => doi(i, 'unitAmount', e.target.value)}
                />
              </td>
              <td>
                <input
                  value={d.amount}
                  inputMode="decimal"
                  style={{ width: '8rem' }}
                  onChange={(e) => doi(i, 'amount', e.target.value)}
                />
              </td>
              <td>
                <button
                  type="button"
                  className="phu"
                  disabled={dong.length === 1}
                  onClick={() => setDong((cu) => cu.filter((_, j) => j !== i))}
                >
                  Bỏ dòng
                </button>
              </td>
            </tr>
          ))}
          <tr>
            <th colSpan={3} scope="row" style={{ textAlign: 'right' }}>
              Tổng ({tienTe(baoGia)})
            </th>
            <td>
              <strong>{tongTam(dong)}</strong>
            </td>
            <td>
              <span className="phu">máy chủ cộng lại khi lưu</span>
            </td>
          </tr>
        </tbody>
      </table>

      <div className="hang" style={{ marginTop: '1rem' }}>
        <button
          type="button"
          className="phu"
          disabled={dong.length >= 40}
          onClick={() =>
            setDong((cu) => [
              ...cu,
              { labelKey: KHOA_DONG_GIA_MAC_DINH, quantity: '', unitAmount: '', amount: '' },
            ])
          }
        >
          + Thêm dòng
        </button>
        <button type="button" disabled={dangLuu} onClick={() => void luu()}>
          {dangLuu ? 'Đang lưu…' : 'Lưu bảng giá'}
        </button>
      </div>
    </section>
  );
}

/**
 * Tiền tệ suy từ **thị trường** của báo giá.
 *
 * Đọc từ `total` khi đã có — đó là con số máy chủ đã ghi. Chưa có thì suy từ
 * thị trường, vì mỗi thị trường có đúng một tiền tệ và không có quy đổi.
 */
function tienTe(baoGia: AdminQuoteDetail): string {
  return baoGia.total?.currency ?? (baoGia.market === 'VN' ? 'VND' : 'DKK');
}

/**
 * Cộng tạm để tư vấn viên soát lại **trước** khi lưu.
 *
 * Con số thật là con số máy chủ trả về sau khi lưu; đây chỉ là bản xem trước.
 * Dòng gõ dở không thành số thì bỏ qua, không hiện `NaN`.
 */
function tongTam(dong: Dong[]): string {
  const tong = dong.reduce((t, d) => {
    const so = Number(d.amount);
    return Number.isFinite(so) ? t + so : t;
  }, 0);
  return new Intl.NumberFormat('vi-VN').format(tong);
}

/* ------------------------------------------------------------ thao tác */

function ThaoTac({
  baoGia,
  onXong,
}: {
  baoGia: AdminQuoteDetail;
  onXong: (b: AdminQuoteDetail) => void;
}) {
  const [chon, setChon] = useState<string | null>(null);
  const [dangGui, setDangGui] = useState(false);
  const [loi, setLoi] = useState('');

  const thaoTac = thaoTacChoPhep(baoGia.status);
  if (thaoTac.length === 0) {
    return null;
  }

  // Gửi một báo giá chưa có dòng nào là một email làm khách mất thời gian.
  // Backend từ chối bằng `409`; ẩn nút ở đây để không ai phải gặp lỗi đó.
  const guiDuoc = baoGia.lines.length > 0;

  async function gui(sang: string) {
    setDangGui(true);
    setLoi('');
    try {
      const moi = await adminApi().changeQuoteStatus({
        reference: baoGia.reference,
        adminQuoteStatusChange: { toStatus: sang as never },
      });
      onXong(moi);
      setChon(null);
    } catch (ex) {
      setLoi(await loiDoiTrangThai(ex));
    } finally {
      setDangGui(false);
    }
  }

  return (
    <section>
      <h2>Thao tác</h2>

      {loi && <p className="loi">{loi}</p>}

      {baoGia.status === 'DRAFT' && !guiDuoc && (
        <p className="trong">
          Dựng bảng giá bên dưới trước đã — không gửi được một báo giá chưa có dòng nào.
        </p>
      )}

      {chon === null && (
        <div className="hang">
          {thaoTac.map((t) => (
            <button
              key={t.sang}
              type="button"
              className={t.nang ? undefined : 'phu'}
              disabled={t.sang === 'SENT' && !guiDuoc}
              onClick={() => {
                setLoi('');
                setChon(t.sang);
              }}
            >
              {t.nhan}
            </button>
          ))}
        </div>
      )}

      {chon !== null && (
        <div className="loc">
          <p>
            <strong>{hauQua(chon, baoGia.quoteValidDays ?? 0)}</strong>
          </p>
          <div className="hang" style={{ marginTop: '0.75rem' }}>
            <button type="button" disabled={dangGui} onClick={() => void gui(chon)}>
              {dangGui ? 'Đang lưu…' : 'Xác nhận'}
            </button>
            <button
              type="button"
              className="phu"
              disabled={dangGui}
              onClick={() => setChon(null)}
            >
              Thôi
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

/**
 * Hai mã lỗi riêng của màn hình này, ngoài bảng dùng chung ở `lib/api.ts`.
 *
 * `QUOTE_NOT_ACCEPTABLE` mang `from`, và câu dựng từ nó nói đúng chuyện đã xảy
 * ra: gần như luôn là **người khác vừa đổi trạng thái** trong lúc màn hình đang
 * mở.
 */
async function loiDoiTrangThai(ex: unknown): Promise<string> {
  const than = await errorInfo(ex);
  if (than?.code === 'QUOTE_EXPIRED') {
    return 'Báo giá đã quá hạn nên không ghi nhận khách đồng ý được. Báo giá không tự gia hạn — khách muốn tiếp thì gửi yêu cầu mới.';
  }
  if (than?.code === 'QUOTE_NOT_ACCEPTABLE') {
    const tu = than.params.from ? tenTrangThai(String(than.params.from)) : 'trạng thái hiện tại';
    return `Báo giá đang ở "${tu}" nên không làm được việc này. Nhiều khả năng người khác vừa đổi — tải lại trang để xem trạng thái mới nhất.`;
  }
  return loiTiengViet(ex);
}
