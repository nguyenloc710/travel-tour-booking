'use client';

import { useState } from 'react';
import { t, type Locale, type Market } from '@travel/i18n';
import { bookingApi, requestScope, translateError } from '@/lib/api';

/**
 * Form yêu cầu báo giá cho `PRIVATE_TOUR` — docs/23 mục 7, docs/05 mục 6.
 *
 * **Đây là CTA của loại sản phẩm này**, không phải một form liên hệ chung: tour
 * riêng không đặt trực tiếp được, nên nút chính của trang chi tiết dẫn tới đây
 * chứ không dẫn tới luồng đặt tour (docs/04).
 *
 * Ba điều khác hẳn luồng đặt tour bốn bước:
 *
 * - **Một bước, một màn hình.** Không có gì để giữ chỗ, không có gì hết hạn, nên
 *   không có lý do chia bước và không có lý do đưa trạng thái vào URL.
 * - **Không hiện giá nào.** Giá của tour riêng là thứ tư vấn viên dựng sau khi
 *   đọc yêu cầu (docs/14 mục 7 quy tắc 5) — hiện một con số ở đây là hứa trước
 *   một thứ chưa ai tính.
 * - **Lịch chặn trước `leadTimeDays`.** Backend vẫn kiểm lại và trả
 *   `LEAD_TIME_NOT_MET`; chặn ở đây chỉ để khách không phải gửi rồi mới biết.
 */
export function YeuCauBaoGia({
  locale,
  market,
  slug,
  leadTimeDays,
}: {
  locale: Locale;
  market: Market;
  slug: string;
  leadTimeDays: number;
}) {
  // Sinh MỘT LẦN cho một lần gửi, không sinh lại ở mỗi lần thử (docs/13 mục 7):
  // khách bấm nút hai lần thì tư vấn viên không được nhận hai yêu cầu giống hệt
  // nhau rồi gọi điện hai lần.
  const [khoaGoiLai] = useState(() => crypto.randomUUID());

  const [dangGui, setDangGui] = useState(false);
  const [loi, setLoi] = useState('');
  const [maBaoGia, setMaBaoGia] = useState('');

  async function gui(su_kien: React.FormEvent<HTMLFormElement>) {
    su_kien.preventDefault();
    setLoi('');
    setDangGui(true);

    const bieuMau = new FormData(su_kien.currentTarget);
    const ngay = String(bieuMau.get('requestedDate') ?? '');
    const loiNhan = String(bieuMau.get('message') ?? '').trim();

    try {
      const bienNhan = await bookingApi().createQuoteRequest({
        ...requestScope(market, locale),
        idempotencyKey: khoaGoiLai,
        quoteRequestInput: {
          productSlug: slug,
          partySize: Number(bieuMau.get('partySize')),
          // Trường rỗng phải BỎ HẲN khỏi thân yêu cầu, không gửi chuỗi rỗng:
          // khách chưa chốt ngày là chuyện bình thường (docs/23 mục 7).
          ...(ngay === '' ? {} : { requestedDate: new Date(ngay) }),
          contactName: String(bieuMau.get('contactName')),
          contactEmail: String(bieuMau.get('contactEmail')),
          contactPhone: String(bieuMau.get('contactPhone')),
          ...(loiNhan === '' ? {} : { message: loiNhan }),
        },
      });
      setMaBaoGia(bienNhan.reference);
    } catch (ex) {
      setLoi(await translateError(locale, ex));
    } finally {
      setDangGui(false);
    }
  }

  if (maBaoGia !== '') {
    return (
      <section className="dat-o" id="bao-gia" aria-live="polite">
        <h2>{t(locale, 'quote.sent.heading')}</h2>
        <p>{t(locale, 'quote.sent.body', { reference: maBaoGia })}</p>
        <p className="facts__note">{t(locale, 'quote.sent.next')}</p>
      </section>
    );
  }

  return (
    <form className="dat" id="bao-gia" onSubmit={gui} noValidate={false}>
      <fieldset className="dat-o">
        <legend>{t(locale, 'quote.heading')}</legend>
        <p>{t(locale, 'quote.intro', { days: leadTimeDays })}</p>

        {loi && (
          <p className="state state--error" role="alert">
            {loi}
          </p>
        )}

        <div className="dat-hang">
          <label htmlFor="q-party">{t(locale, 'quote.partySize')}</label>
          <input id="q-party" name="partySize" type="number" min={1} max={40} defaultValue={2} required />
        </div>

        <div className="dat-hang">
          <label htmlFor="q-date">{t(locale, 'quote.requestedDate')}</label>
          {/* `min` chặn ngay ở lịch của trình duyệt — khách không phải gửi rồi
              mới biết. Backend vẫn kiểm lại: chặn ở một phía là chặn được đúng
              những người dùng trình duyệt. */}
          <input id="q-date" name="requestedDate" type="date" min={somNhat(leadTimeDays)} />
        </div>
        <p className="facts__note">{t(locale, 'quote.requestedDate.help')}</p>

        <div className="dat-hang">
          <label htmlFor="q-name">{t(locale, 'quote.name')}</label>
          <input id="q-name" name="contactName" type="text" maxLength={160} required />
        </div>

        <div className="dat-hang">
          <label htmlFor="q-email">{t(locale, 'booking.email')}</label>
          <input id="q-email" name="contactEmail" type="email" required />
        </div>

        <div className="dat-hang">
          <label htmlFor="q-phone">{t(locale, 'booking.phone')}</label>
          <input id="q-phone" name="contactPhone" type="tel" minLength={5} maxLength={32} required />
        </div>

        <div className="dat-hang dat-hang--doc">
          <label htmlFor="q-message">{t(locale, 'quote.message')}</label>
          <textarea id="q-message" name="message" rows={4} maxLength={2000} />
        </div>
        <p className="facts__note">{t(locale, 'quote.message.help')}</p>
      </fieldset>

      <div className="dat-nut">
        <button type="submit" disabled={dangGui}>
          {dangGui ? t(locale, 'quote.submitting') : t(locale, 'quote.submit')}
        </button>
      </div>
    </form>
  );
}

/** Ngày sớm nhất khách chọn được, ở dạng `YYYY-MM-DD` mà `<input type="date">` nhận. */
function somNhat(leadTimeDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + leadTimeDays);
  return d.toISOString().slice(0, 10);
}
