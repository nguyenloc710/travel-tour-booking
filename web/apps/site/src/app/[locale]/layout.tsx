import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { isLocale, locales, t, type Locale } from '@travel/i18n';
import { resolveMarket } from '@/lib/market';
import { MarketBanner } from '@/components/MarketBanner';
import {
  duongDanBlog,
  duongDanDiemDen,
  duongDanLienHe,
  duongDanListing,
  duongDanSuKien,
} from '@/lib/routes';
import '../globals.css';

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
    <html lang={locale}>
      <body>
        <a className="skip-link" href="#noi-dung">
          {t(locale, 'a11y.skipToContent')}
        </a>

        <header className="site-header">
          <p className="site-header__name">{t(locale, 'site.name')}</p>
          {/* Số điện thoại và giờ mở cửa hiện THƯỜNG TRỰC — khách lớn tuổi
              Bắc Âu gọi điện nhiều hơn điền form. web/CLAUDE.md mục 6. */}
          <p className="site-header__contact">
            <a href={`tel:${t(locale, 'site.phone').replace(/\s/g, '')}`}>
              {t(locale, 'site.phone')}
            </a>
            <span>{t(locale, 'site.openingHours')}</span>
          </p>
        </header>

        {/* Menu chính. Không có nó thì blog, sự kiện và liên hệ tồn tại mà
            không ai tìm ra — trang dựng được không đồng nghĩa với trang đến
            được. Liên kết thật, không phải menu đổ xuống cần hover: hover là
            điều kiện duy nhất để lộ thông tin thì khách dùng cảm ứng mất lối
            (web/CLAUDE.md mục 6). */}
        <nav className="site-nav" aria-label={t(locale, 'nav.label')}>
          <div className="site-nav__trong">
            <Link href={duongDanListing(locale)}>{t(locale, 'nav.products')}</Link>
            <Link href={duongDanDiemDen(locale)}>{t(locale, 'nav.destinations')}</Link>
            <Link href={duongDanBlog(locale)}>{t(locale, 'nav.blog')}</Link>
            <Link href={duongDanSuKien(locale)}>{t(locale, 'nav.events')}</Link>
            <Link href={duongDanLienHe(locale)}>{t(locale, 'nav.contact')}</Link>
          </div>
        </nav>

        {!laMacDinh && <MarketBanner locale={locale as Locale} market={market} />}

        <main id="noi-dung">{children}</main>
      </body>
    </html>
  );
}
