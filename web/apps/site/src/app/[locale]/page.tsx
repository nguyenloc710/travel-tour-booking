import { Suspense } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { notFound } from 'next/navigation';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatDate, formatNumber } from '@travel/ui';
import type { Lecture, PostPage, ProductPage, ProductType } from '@travel/api-client';
import { lecturesApi, postsApi, productsApi, regionsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import {
  duongDanBlog,
  duongDanListing,
  duongDanSuKien,
  duongDanTimTour,
} from '@/lib/routes';
import { ProductCard } from '@/components/ProductCard';

/**
 * Trang chủ (R1 của `docs/20`).
 *
 * Thứ tự khối đi theo thứ tự khách quyết định, không theo thứ tự chúng tôi thấy
 * quan trọng: **họ đang ở đâu** (khối mở đầu), **đi kiểu gì** (hình thức), **đi
 * đâu** (miền), rồi hai lối vào mềm — bài viết và buổi thuyết trình.
 *
 * Khối cuối không phải trang trí: `docs/21` mục 1 nói khách quyết định mua
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
      <KhoiMoDau locale={locale} />
      <BangLoiHua locale={locale} />

      <section className="doan doan--diu">
        <div className="doan__trong">
          <h2>{t(locale, 'home.types')}</h2>
          <p className="doan__dan">{t(locale, 'home.types.intro')}</p>
          <HinhThuc locale={locale} />
        </div>
      </section>

      <section className="doan">
        <div className="doan__trong">
          <h2>{t(locale, 'home.popular')}</h2>
          {/* Trạng thái ĐANG TẢI là skeleton, không phải vòng xoay — web/CLAUDE.md 5.3 */}
          <Suspense fallback={<LuoiCho khung />}>
            <TourNoiBat locale={locale} />
          </Suspense>
        </div>
      </section>

      <section className="doan doan--diu">
        <div className="doan__trong">
          <h2>{t(locale, 'home.regions')}</h2>
          <p className="doan__dan">{t(locale, 'home.regions.intro')}</p>
          <Suspense fallback={<LuoiCho />}>
            <DanhSachMien locale={locale} />
          </Suspense>
        </div>
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
 * Khối mở đầu: ảnh phủ kín, chữ đè lên.
 *
 * Ảnh dùng `priority` vì nó là phần tử lớn nhất trong khung nhìn đầu tiên —
 * để Next tải lười thì chỉ số LCP đo đúng cái khoảng trắng đó.
 *
 * Chữ đè lên ảnh chỉ đọc được nhờ lớp phủ tối, và lớp phủ ấy **nằm trong chính
 * tệp SVG** (xem `scripts/sinh-anh-mau.py`) chứ không chỉ trong CSS: ảnh thật
 * sau này cũng phải có vùng tối ở dưới, và ghi ra ở cả hai chỗ để người thay
 * ảnh biết mình đang phải giữ điều gì.
 */
function KhoiMoDau({ locale }: { locale: Locale }) {
  return (
    <section className="mo-dau">
      <Image
        className="mo-dau__anh"
        src="/img/hero.svg"
        alt={t(locale, 'home.hero.imageAlt')}
        width={1600}
        height={900}
        priority
        unoptimized
      />

      <div className="mo-dau__chu">
        <p className="mo-dau__nhan">{t(locale, 'home.hero.eyebrow')}</p>
        <h1>{t(locale, 'home.heading')}</h1>
        <p className="mo-dau__dan">{t(locale, 'home.intro')}</p>

        <p className="mo-dau__nut">
          <Link className="nut" href={duongDanListing(locale)}>
            {t(locale, 'home.browse')}
          </Link>
          <Link className="nut nut--phu" href={duongDanTimTour(locale)}>
            {t(locale, 'home.finder')}
          </Link>
        </p>
      </div>
    </section>
  );
}

/** Bốn lời hứa. Chữ, không phải biểu tượng: biểu tượng một mình không dịch được. */
function BangLoiHua({ locale }: { locale: Locale }) {
  const muc = ['leader', 'small', 'phone', 'contact'] as const;

  return (
    <section className="loi-hua" aria-label={t(locale, 'site.tagline')}>
      <ul className="loi-hua__trong">
        {muc.map((m) => (
          <li key={m}>
            <p className="loi-hua__ten">{t(locale, `trust.${m}.title`)}</p>
            <p className="loi-hua__mo-ta">{t(locale, `trust.${m}.body`)}</p>
          </li>
        ))}
      </ul>
    </section>
  );
}

/**
 * Sáu hình thức đi của `docs/04`.
 *
 * Danh sách này KHÔNG gọi API: sáu loại sản phẩm là hằng số của nghiệp vụ, nằm
 * trong `openapi.yaml` dưới dạng enum, và thêm loại thứ bảy là một thay đổi hợp
 * đồng chứ không phải một dòng dữ liệu. Kiểu `ProductType` sinh từ spec canh
 * chỗ này: thêm loại vào spec mà quên ở đây thì `pnpm typecheck` đỏ.
 *
 * Không hiện số sản phẩm mỗi loại — chưa có endpoint nào đếm theo loại, và một
 * con số đếm sai còn tệ hơn không có con số (`CLAUDE.md` quy tắc 1).
 */
function HinhThuc({ locale }: { locale: Locale }) {
  const loai: ProductType[] = [
    'GROUP_TOUR',
    'INDIVIDUAL_PACKAGE',
    'PRIVATE_TOUR',
    'CRUISE',
    'COMBO',
    'DAY_TOUR',
  ];

  return (
    <ul className="hinh-thuc">
      {loai.map((l) => (
        <li key={l} className="hinh-thuc__o">
          <h3>
            <Link href={duongDanListing(locale, new URLSearchParams({ productType: l }))}>
              {t(locale, `productType.${l}`)}
            </Link>
          </h3>
          <p>{t(locale, `home.type.${l}`)}</p>
        </li>
      ))}
    </ul>
  );
}

/** Sáu sản phẩm đầu của thị trường đang xem. */
async function TourNoiBat({ locale }: { locale: Locale }) {
  const { market } = await resolveMarket(locale);

  let trang: ProductPage;
  try {
    trang = await productsApi().listProducts({ ...requestScope(market, locale), size: 6 });
  } catch (loi) {
    console.error('[api] listProducts (trang chủ) thất bại', loi);
    return <TrangThaiLoi locale={locale} />;
  }

  if (trang.items.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'state.empty.title')}</p>
        <p>{t(locale, 'state.empty.help')}</p>
      </div>
    );
  }

  return (
    <>
      <ul className="product-grid">
        {trang.items.map((sp) => (
          <li key={sp.slug}>
            <ProductCard product={sp} locale={locale} />
          </li>
        ))}
      </ul>

      <p className="doan__them">
        <Link className="nut nut--phu" href={duongDanListing(locale)}>
          {/* Con số đếm TỪ DỮ LIỆU trong phạm vi (market, locale) đang xem —
              "Xem tất cả 92 tour" không bao giờ được viết cứng. */}
          {t(locale, trang.totalItems === 1 ? 'products.total.one' : 'products.total.many', {
            count: formatNumber(trang.totalItems, locale),
          })}
        </Link>
      </p>
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
    return <TrangThaiLoi locale={locale} />;
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
    <section className="doan">
      <div className="doan__trong">
        <h2>{t(locale, 'home.latestPosts')}</h2>
        <p className="doan__dan">{t(locale, 'home.blog.intro')}</p>

        <ul className="bai-luoi">
          {trang.items.map((b) => (
            <li key={b.slug} className="bai-the">
              {/* `heroImage` và `publishedAt` là trường TUỲ CHỌN trong hợp đồng:
                  một bài chưa có ảnh bìa vẫn là một bài hợp lệ. Kiểu sinh từ
                  spec bắt phải kiểm, và đó là chỗ kiểu sinh ra kiếm được tiền
                  ăn của nó — không có nó thì lỗi này ra tới trình duyệt. */}
              {b.heroImage !== undefined && (
                <Image
                  className="bai-the__anh"
                  src={b.heroImage}
                  alt={b.title}
                  width={640}
                  height={360}
                  unoptimized
                />
              )}
              <div className="bai-the__than">
                <h3>
                  <Link href={duongDanBlog(locale, b.slug)}>{b.title}</Link>
                </h3>
                <p>{b.excerpt}</p>
                {b.publishedAt !== undefined && (
                  <p className="bai-the__ngay">{formatDate(b.publishedAt, locale)}</p>
                )}
              </div>
            </li>
          ))}
        </ul>

        <p className="doan__them">
          <Link className="nut nut--phu" href={duongDanBlog(locale)}>
            {t(locale, 'home.seeAll')}
          </Link>
        </p>
      </div>
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
    <section className="doan doan--diu">
      <div className="doan__trong">
        <h2>{t(locale, 'home.nextEvents')}</h2>

        <ul className="su-kien-dai">
          {buoi.slice(0, 3).map((b) => (
            <li key={b.id}>
              <p className="su-kien-dai__ngay">{formatDate(b.eventDate, locale)}</p>
              <p className="su-kien-dai__ten">{b.title}</p>
              <p className="su-kien-dai__noi">
                {b.venue !== undefined ? `${b.city} · ${b.venue}` : b.city}
              </p>
            </li>
          ))}
        </ul>

        <p className="doan__them">
          <Link className="nut nut--phu" href={duongDanSuKien(locale)}>
            {t(locale, 'home.seeAll')}
          </Link>
        </p>
      </div>
    </section>
  );
}

function LuoiCho({ khung = false }: { khung?: boolean }) {
  return (
    <ul className={khung ? 'product-grid' : 'mien-luoi'} aria-hidden="true">
      {[0, 1, 2, 3, 4, 5].map((i) => (
        <li key={i}>
          {khung && <span className="skeleton-bar skeleton-bar--image" />}
          <span className="skeleton-bar" />
          <span className="skeleton-bar skeleton-bar--short" />
        </li>
      ))}
    </ul>
  );
}

function TrangThaiLoi({ locale }: { locale: Locale }) {
  return (
    <div className="state state--error" role="alert">
      <p className="state__title">{t(locale, 'state.error.title')}</p>
      <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
    </div>
  );
}
