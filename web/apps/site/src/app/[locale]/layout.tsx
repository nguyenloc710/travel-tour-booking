import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { Be_Vietnam_Pro, Playfair_Display } from 'next/font/google';
import { isLocale, locales, t, type Locale } from '@travel/i18n';
import { resolveMarket } from '@/lib/market';
import { MarketBanner } from '@/components/MarketBanner';
import {
  duongDanBlog,
  duongDanDiemDen,
  duongDanLienHe,
  duongDanListing,
  duongDanSuKien,
  duongDanTimTour,
} from '@/lib/routes';
import '../globals.css';

/**
 * Hai bộ chữ, và cả hai phải dựng được **cả `æ ø å` lẫn dấu tiếng Việt** —
 * `docs/21` mục 3.1. Đó là lý do danh sách `subsets` dưới đây có cả
 * `latin-ext` lẫn `vietnamese`: thiếu một trong hai thì trình duyệt lấy chữ
 * thay thế cho đúng những ký tự ấy, và trang hiện ra hai kiểu chữ lẫn lộn ở
 * đúng những từ quan trọng nhất.
 *
 * Be Vietnam Pro dựng riêng cho tiếng Việt nên dấu không bị cụt ở cỡ nhỏ.
 * Playfair Display chỉ dùng cho tiêu đề — chữ có chân ở thân bài làm khó đọc
 * trên màn hình cũ, và khách của trang này là người lớn tuổi (`docs/21` mục 1).
 *
 * `next/font` tải chữ về lúc BUILD và tự phục vụ từ máy chủ của mình. Hệ quả
 * phải biết: CI cần mạng khi build. Đổi lại không có lượt gọi nào sang Google
 * lúc khách xem trang — nhúng thẳng `fonts.googleapis.com` là gửi địa chỉ IP
 * của khách sang bên thứ ba, đúng việc đã bị phạt ở châu Âu (`docs/31`).
 */
const chuTieuDe = Playfair_Display({
  subsets: ['latin', 'latin-ext', 'vietnamese'],
  weight: ['500', '600'],
  variable: '--font-tieu-de',
  display: 'swap',
});

const chuThan = Be_Vietnam_Pro({
  subsets: ['latin', 'latin-ext', 'vietnamese'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-than',
  display: 'swap',
});

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  if (!isLocale(locale)) return {};
  return {
    title: t(locale, 'site.name'),
    description: t(locale, 'site.tagline'),
    alternates: {
      // Trang chưa có bản dịch thì KHÔNG khai báo hreflang cho locale đó —
      // docs/02 mục 5.2. Trang chủ luôn có cả hai.
      languages: { da: '/da', vi: '/vi', 'x-default': '/da' },
    },
  };
}

export default async function LocaleLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  if (!isLocale(locale)) notFound();

  const { market, laMacDinh } = await resolveMarket(locale);

  return (
    <html lang={locale} className={`${chuTieuDe.variable} ${chuThan.variable}`}>
      <body>
        <a className="skip-link" href="#noi-dung">
          {t(locale, 'a11y.skipToContent')}
        </a>

        {/* Đầu trang KHÔNG dính. Thanh tab của trang chi tiết mới là thứ dính
            (xem globals.css), và hai thanh dính chồng nhau ăn mất một phần ba
            màn hình điện thoại. Số điện thoại vẫn có mặt suốt trang chi tiết
            nhờ thanh dính đáy của docs/05 mục 4. */}
        <header className="dau-trang">
          <div className="dau-trang__hang">
            <p className="dau-trang__ten">
              <Link href={`/${locale}`}>{t(locale, 'site.name')}</Link>
              <span className="dau-trang__khau-hieu">{t(locale, 'site.tagline')}</span>
            </p>

            <div className="dau-trang__phai">
              <ChuyenNgonNgu locale={locale} />

              <p className="dau-trang__lien-he">
                <a
                  className="dau-trang__so"
                  href={`tel:${t(locale, 'site.phone').replace(/\s/g, '')}`}
                >
                  {t(locale, 'site.phone')}
                </a>
                <span className="dau-trang__gio">{t(locale, 'site.openingHours')}</span>
              </p>
            </div>
          </div>

          {/* Menu chính. Liên kết thật, không phải menu đổ xuống cần hover:
              hover là điều kiện duy nhất để lộ thông tin thì khách dùng cảm
              ứng mất lối (web/CLAUDE.md mục 6). Cuộn ngang được trên màn hình
              hẹp — thà cuộn còn hơn giấu sau một nút ba gạch. */}
          <nav className="site-nav" aria-label={t(locale, 'nav.label')}>
            <div className="site-nav__trong">
              <Link href={duongDanListing(locale)}>{t(locale, 'nav.products')}</Link>
              <Link href={duongDanDiemDen(locale)}>{t(locale, 'nav.destinations')}</Link>
              <Link href={duongDanTimTour(locale)}>{t(locale, 'nav.tourFinder')}</Link>
              <Link href={duongDanBlog(locale)}>{t(locale, 'nav.blog')}</Link>
              <Link href={duongDanSuKien(locale)}>{t(locale, 'nav.events')}</Link>
              <Link href={duongDanLienHe(locale)}>{t(locale, 'nav.contact')}</Link>
            </div>
          </nav>
        </header>

        {!laMacDinh && <MarketBanner locale={locale as Locale} market={market} />}

        <main id="noi-dung">{children}</main>

        <ChanTrang locale={locale} />
      </body>
    </html>
  );
}

/**
 * Đổi ngôn ngữ — dẫn về **trang chủ** của locale kia, không dịch tại chỗ.
 *
 * Dịch tại chỗ nghe hợp lý hơn và sai nhiều hơn: đoạn đường dẫn khác nhau theo
 * locale (`/da/rejser/…` với `/vi/tour/…`) và slug của bản ghi cũng khác, nên
 * dịch URL đòi hỏi tra ngược slug qua API cho từng trang. Tệ hơn: bản ghi có
 * thể **không tồn tại** ở locale kia — chính sách không-fallback bắt nó trả 404
 * (`CLAUDE.md` điều 3). Đưa khách từ một trang đang đọc được sang một trang 404
 * là cách chắc chắn nhất để mất khách đó.
 *
 * Về trang chủ thì luôn đúng. Dịch URL từng trang là việc của `docs/02` mục 5.2
 * khi có endpoint tra slug hai chiều.
 */
function ChuyenNgonNgu({ locale }: { locale: Locale }) {
  return (
    <p className="doi-ngu" aria-label={t(locale, 'a11y.languageSwitch')}>
      {locales.map((ma) => (
        <Link
          key={ma}
          href={`/${ma}`}
          hrefLang={ma}
          aria-current={ma === locale ? 'true' : undefined}
        >
          {t(locale, `locale.${ma}`)}
        </Link>
      ))}
    </p>
  );
}

function ChanTrang({ locale }: { locale: Locale }) {
  return (
    <footer className="chan-trang">
      <div className="chan-trang__cot">
        <div>
          <p className="chan-trang__ten">{t(locale, 'footer.company')}</p>
          <p>{t(locale, 'footer.blurb')}</p>
          <p>
            <a href={`tel:${t(locale, 'site.phone').replace(/\s/g, '')}`}>
              {t(locale, 'site.phone')}
            </a>
            <br />
            <a href={`mailto:${t(locale, 'site.email')}`}>{t(locale, 'site.email')}</a>
            <br />
            {t(locale, 'site.openingHours')}
          </p>
        </div>

        <nav aria-labelledby="chan-tour">
          <p className="chan-trang__muc" id="chan-tour">
            {t(locale, 'footer.explore')}
          </p>
          <Link href={duongDanListing(locale)}>{t(locale, 'nav.products')}</Link>
          <Link href={duongDanDiemDen(locale)}>{t(locale, 'nav.destinations')}</Link>
          <Link href={duongDanTimTour(locale)}>{t(locale, 'nav.tourFinder')}</Link>
        </nav>

        <nav aria-labelledby="chan-ve">
          <p className="chan-trang__muc" id="chan-ve">
            {t(locale, 'footer.about')}
          </p>
          <Link href={duongDanBlog(locale)}>{t(locale, 'nav.blog')}</Link>
          <Link href={duongDanSuKien(locale)}>{t(locale, 'nav.events')}</Link>
          <Link href={duongDanLienHe(locale)}>{t(locale, 'footer.contact')}</Link>
        </nav>
      </div>

      {/* Câu miễn trừ về giá lặp lại ở chân trang, không chỉ ở cạnh từng con số:
          yêu cầu pháp lý (CLAUDE.md điều 4 của mục quy tắc bắt buộc). */}
      <p className="chan-trang__luat">{t(locale, 'footer.legal')}</p>
    </footer>
  );
}
