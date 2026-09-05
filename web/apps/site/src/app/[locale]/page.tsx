import { Suspense } from 'react';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatDate, formatNumber } from '@travel/ui';
import type { Lecture, PostPage } from '@travel/api-client';
import { lecturesApi, postsApi, regionsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import {
  duongDanBlog,
  duongDanListing,
  duongDanSuKien,
  duongDanTimTour,
} from '@/lib/routes';

/**
 * Trang chủ (R1 của `docs/20`).
 *
 * Ba khối, theo đúng thứ tự khách quyết định: **họ là ai** (khối mở đầu), **đi
 * đâu** (miền), rồi **hai lối vào mềm** — bài viết mới và buổi thuyết trình sắp
 * tới. Khối cuối không phải trang trí: `docs/21` mục 1 nói khách quyết định mua
 * **sau khi gọi điện**, và một buổi nói chuyện ở København là bước trước cuộc
 * gọi đó.
 *
 * `/site-info` của `docs/20` chưa có trong hợp đồng (`docs/13` mục 9.1 mới nói
 * bốn chữ), nên phần thị thực, mùa và lệch giờ chưa lên được trang này.
 */
export default async function HomePage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  if (!isLocale(locale)) notFound();

  return (
    <>
      <section className="hero">
        <h1>{t(locale, 'home.heading')}</h1>
        <p>{t(locale, 'home.intro')}</p>

        <p className="hero__nut">
          <Link className="nut" href={duongDanListing(locale)}>
            {t(locale, 'home.browse')}
          </Link>
          <Link className="nut nut--phu" href={duongDanTimTour(locale)}>
            {t(locale, 'home.finder')}
          </Link>
        </p>
      </section>

      <section>
        <h2>{t(locale, 'home.regions')}</h2>
        {/* Trạng thái ĐANG TẢI là skeleton, không phải vòng xoay — web/CLAUDE.md mục 5.3 */}
        <Suspense fallback={<LuoiCho />}>
          <DanhSachMien locale={locale} />
        </Suspense>
      </section>

      <Suspense fallback={null}>
        <BaiVietMoi locale={locale} />
      </Suspense>

      <Suspense fallback={null}>
        <SuKienSapToi locale={locale} />
      </Suspense>
    </>
  );
}

/**
 * Miền, kèm số sản phẩm.
 *
 * Mỗi miền là một **liên kết** dẫn thẳng sang danh sách đã lọc, không phải một
 * dòng chữ: khách bấm vào tên miền là hành vi mặc định, và một danh sách không
 * bấm được là một danh sách bắt người ta tự tìm đường.
 */
async function DanhSachMien({ locale }: { locale: Locale }) {
  const { market } = await resolveMarket(locale);

  let mien;
  try {
    mien = await regionsApi().listRegions(requestScope(market, locale));
  } catch (loi) {
    // KHÔNG hiện mã lỗi ra khách. Ghi log, hiện câu chung kèm số điện thoại.
    console.error('[api] listRegions thất bại', loi);
    return (
      <div className="state state--error" role="alert">
        <p className="state__title">{t(locale, 'state.error.title')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  // Trạng thái RỖNG kèm hướng dẫn hành động, không phải một dòng "không có gì".
  if (mien.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'state.empty.title')}</p>
        <p>{t(locale, 'state.empty.help')}</p>
      </div>
    );
  }

  return (
    <ul className="mien-luoi">
      {mien.map((m) => (
        <li key={m.slug}>
          <Link href={duongDanListing(locale, new URLSearchParams({ region: m.slug }))}>
            <strong>{m.name}</strong>
            {/* Số đếm TỪ DỮ LIỆU, trong phạm vi (market, locale) đang xem. Hai
                locale ra hai con số khác nhau vì bản ghi chưa dịch bị ẩn hoàn
                toàn — đó là lý do con số này không được hardcode. */}
            <span className="region-list__count">
              {t(locale, 'region.productCount', {
                count: formatNumber(m.productCount, locale),
              })}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

/**
 * Ba bài viết mới nhất.
 *
 * Khối phụ: hỏng thì **im lặng biến mất**, không dựng băng lỗi. Trang chủ vẫn
 * làm được việc của nó khi blog không tải được, và một băng đỏ ở đây làm khách
 * tưởng cả trang hỏng.
 */
async function BaiVietMoi({ locale }: { locale: Locale }) {
  const { market } = await resolveMarket(locale);

  let trang: PostPage;
  try {
    trang = await postsApi().listPosts({ ...requestScope(market, locale), size: 3 });
  } catch (loi) {
    console.error('[api] listPosts (trang chủ) thất bại', loi);
    return null;
  }

  if (trang.items.length === 0) {
    return null;
  }

  return (
    <section>
      <h2>{t(locale, 'home.latestPosts')}</h2>
      <ul className="mien-luoi">
        {trang.items.map((b) => (
          <li key={b.slug}>
            <Link href={duongDanBlog(locale, b.slug)}>
              <strong>{b.title}</strong>
              <span className="region-list__count">{b.excerpt}</span>
            </Link>
          </li>
        ))}
      </ul>
      <p>
        <Link href={duongDanBlog(locale)}>{t(locale, 'home.seeAll')}</Link>
      </p>
    </section>
  );
}

/** Buổi thuyết trình sắp tới — endpoint chỉ trả buổi chưa diễn ra. */
async function SuKienSapToi({ locale }: { locale: Locale }) {
  const { market } = await resolveMarket(locale);

  let buoi: Lecture[];
  try {
    buoi = await lecturesApi().listLectures(requestScope(market, locale));
  } catch (loi) {
    console.error('[api] listLectures (trang chủ) thất bại', loi);
    return null;
  }

  if (buoi.length === 0) {
    return null;
  }

  return (
    <section>
      <h2>{t(locale, 'home.nextEvents')}</h2>
      <ul className="mien-luoi">
        {buoi.slice(0, 3).map((b) => (
          <li key={b.id}>
            <Link href={duongDanSuKien(locale)}>
              <strong>{b.title}</strong>
              <span className="region-list__count">
                {formatDate(b.eventDate, locale)} · {b.city}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

function LuoiCho() {
  return (
    <ul className="mien-luoi" aria-hidden="true">
      {[0, 1, 2, 3].map((i) => (
        <li key={i}>
          <span className="skeleton-bar" />
          <span className="skeleton-bar skeleton-bar--short" />
        </li>
      ))}
    </ul>
  );
}
