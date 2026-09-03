import { Suspense } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import type { Metadata } from 'next';
import { notFound, permanentRedirect } from 'next/navigation';
import { isLocale, t, type Locale } from '@travel/i18n';
import { formatMoney, formatNumber, PriceFrom } from '@travel/ui';
import type { Departure, Destination, ProductDetail } from '@travel/api-client';
import { bookingApi, destinationsApi, laKhongTimThay, productsApi, requestScope } from '@/lib/api';
import { resolveMarket } from '@/lib/market';
import {
  duongDanChiTiet,
  duongDanDiemDen,
  duongDanListing,
  laBookingSegment,
  laConfirmationSegment,
  laDestinationsSegment,
  laProductsSegment,
} from '@/lib/routes';
import { DatTour, type TrangThai } from '@/components/DatTour';
import { ProductCard } from '@/components/ProductCard';
import { ProductFacts } from '@/components/ProductFacts';

type Params = Promise<{ locale: string; section: string; slug: string }>;
type Search = Promise<Record<string, string | string[] | undefined>>;

/**
 * Lấy sản phẩm, hoặc 404.
 *
 * **404 ở đây gộp ba tình huống có chủ ý**: không tồn tại, chưa dịch sang
 * locale này, chưa bán ở thị trường này. Backend đã gộp sẵn (docs/13 mục 5.1),
 * và frontend **không** được đi tìm bản `da` để lấp chỗ trống — đó chính là
 * chính sách không-fallback của docs/02 mục 4.
 */
/**
 * Slug này có phải slug **cũ** không — nếu phải thì chuyển hướng vĩnh viễn.
 *
 * Đổi slug làm mọi liên kết cũ chết: liên kết trong email đã gửi, trong bài viết
 * của người khác, trong dấu trang của khách. Bảng `slug_history` giữ slug cũ, và
 * endpoint `/redirects` là đường đọc của nó.
 *
 * **Chỉ gọi sau khi đã nhận `404`**, không gọi trước: đường đi bình thường không
 * được trả giá cho một trường hợp hiếm.
 *
 * Backend đã kiểm bản ghi đích có xem được ở `(market, locale)` này không, nên
 * nếu nó trả slug thì slug đó chắc chắn mở được. Frontend không kiểm lại.
 *
 * **`permanentRedirect` phát 308, không phải 301.** Cả hai đều là "chuyển vĩnh
 * viễn" và công cụ tìm kiếm xử lý như nhau; 308 chặt hơn ở chỗ nó cấm đổi
 * phương thức HTTP. Đây là thứ Next cho sẵn ở tầng trang; muốn đúng 301 thì phải
 * chuyển việc này xuống `proxy.ts`, và khi đó **mọi** request phải trả giá cho
 * một trường hợp hiếm.
 */
async function chuyenHuongNeuSlugCu(
  locale: Locale,
  loai: 'PRODUCT' | 'DESTINATION',
  slug: string,
): Promise<never> {
  const { market } = await resolveMarket(locale);

  let slugMoi: string;
  try {
    const kq = await productsApi().resolveSlug({
      ...requestScope(market, locale),
      type: loai as never,
      slug,
    });
    slugMoi = kq.slug;
  } catch (loi) {
    // CHỈ `404` mới nghĩa là "không phải slug cũ". `catch` trống ở đây nuốt luôn
    // lỗi mạng và lỗi lập trình, biến mọi sự cố thành một trang 404 im lặng —
    // và bản đầu tiên của hàm này đúng là như vậy, nên tra ra mất một lúc.
    if (!laKhongTimThay(loi)) {
      throw loi;
    }
    // Không log: URL gõ sai là đường đi bình thường, không phải sự cố.
    notFound();
  }

  permanentRedirect(
    loai === 'PRODUCT' ? duongDanChiTiet(locale, slugMoi) : duongDanDiemDen(locale, slugMoi),
  );
}

/**
 * Lấy điểm đến, hoặc 404 — cùng chính sách với sản phẩm.
 *
 * `404` gộp ba tình huống: không tồn tại, chưa dịch cho locale này, hoặc miền
 * chứa nó đã bị xoá mềm. Backend gộp sẵn, và frontend **không** đi tìm bản `da`
 * để lấp chỗ trống.
 */
async function layDiemDen(locale: Locale, slug: string): Promise<Destination> {
  const { market } = await resolveMarket(locale);
  try {
    return await destinationsApi().getDestination({ ...requestScope(market, locale), slug });
  } catch (loi) {
    if (laKhongTimThay(loi)) {
      await chuyenHuongNeuSlugCu(locale, 'DESTINATION', slug);
    }
    throw loi;
  }
}

async function laySanPham(locale: Locale, slug: string): Promise<ProductDetail> {
  const { market } = await resolveMarket(locale);
  try {
    return await productsApi().getProduct({ ...requestScope(market, locale), slug });
  } catch (loi) {
    // 404 chưa chắc là "không có": có thể slug này vừa đổi tên.
    if (laKhongTimThay(loi)) {
      await chuyenHuongNeuSlugCu(locale, 'PRODUCT', slug);
    }
    // Lỗi mạng không phải "không tồn tại" — ném tiếp để error boundary lo,
    // đừng biến một sự cố backend thành trang 404 và mất khách thật.
    throw loi;
  }
}

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { locale, section, slug } = await params;
  if (!isLocale(locale)) return {};

  if (laDestinationsSegment(locale, section)) {
    const diemDen = await layDiemDen(locale, slug);
    return {
      title: `${diemDen.name} · ${t(locale, 'site.name')}`,
      description: diemDen.summary,
      alternates: { canonical: duongDanDiemDen(locale, slug) },
    };
  }

  // Đặt tour và xác nhận: `no-store`, và KHÔNG cho công cụ tìm kiếm chạm vào —
  // hai trang này chứa trạng thái của một lần đặt cụ thể (docs/20 mục 2).
  if (laBookingSegment(locale, section) || laConfirmationSegment(locale, section)) {
    return { robots: { index: false, follow: false } };
  }

  if (!laProductsSegment(locale, section)) return {};

  const sanPham = await laySanPham(locale, slug);
  return {
    title: `${sanPham.title} · ${t(locale, 'site.name')}`,
    description: sanPham.shortDescription,
    // KHÔNG khai báo hreflang cho locale kia: slug phụ thuộc locale và trang
    // này không biết bản dịch bên kia có tồn tại hay không. Khai bừa một URL
    // trả 404 còn tệ hơn không khai (docs/02 mục 5.2).
  };
}

export default async function ProductDetailPage({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: Search;
}) {
  const { locale, section, slug } = await params;
  if (!isLocale(locale)) notFound();

  if (laDestinationsSegment(locale, section)) {
    return <DestinationDetailPage locale={locale} slug={slug} />;
  }
  if (laBookingSegment(locale, section)) {
    return <DatTourPage locale={locale} slug={slug} thamSo={await searchParams} />;
  }
  if (laConfirmationSegment(locale, section)) {
    return <XacNhanPage locale={locale} reference={slug} thamSo={await searchParams} />;
  }
  if (!laProductsSegment(locale, section)) notFound();

  const sanPham = await laySanPham(locale, slug);

  return (
    <article className="detail">
      <p className="detail__breadcrumb">
        <Link href={duongDanListing(locale)}>{t(locale, 'detail.backToList')}</Link>
      </p>

      <header className="detail__header">
        <p className="detail__meta">
          <span className="badge">{t(locale, `productType.${sanPham.productType}`)}</span>
          <span>
            {sanPham.destination.name} · {sanPham.region.name}
          </span>
          {sanPham.durationDays !== undefined && (
            <span>
              {t(locale, 'products.duration', {
                days: formatNumber(sanPham.durationDays, locale),
              })}
            </span>
          )}
        </p>

        <h1>{sanPham.title}</h1>
        <p className="detail__summary">{sanPham.shortDescription}</p>

        {sanPham.priceFrom ? (
          <PriceFrom price={sanPham.priceFrom} locale={locale} />
        ) : (
          <p className="price-from price-from--contact">{t(locale, 'products.contactForPrice')}</p>
        )}

        {sanPham.productType === 'PRIVATE_TOUR' && (
          <p className="detail__cta">
            <strong>{t(locale, 'detail.private.cta')}</strong>
          </p>
        )}
      </header>

      <Image
        className="detail__image"
        src={sanPham.heroImage}
        alt={sanPham.heroImageAlt}
        width={1200}
        height={640}
        priority
        unoptimized
      />

      <div className="detail__body">
        {/* Từng đoạn văn một, không phải một khối HTML: nội dung do biên tập
            viên nhập, và HTML tự do từ CSDL là lỗ chèn mã chờ sẵn. */}
        {sanPham.longDescription.map((doan, i) => (
          <p key={i}>{doan}</p>
        ))}
      </div>

      <section className="why">
        <h2>{t(locale, 'detail.whyChooseThis')}</h2>
        <ul>
          {sanPham.whyChooseThis.map((ly_do) => (
            <li key={ly_do}>{ly_do}</li>
          ))}
        </ul>
      </section>

      <ProductFacts product={sanPham} locale={locale} />

      {sanPham.mapImage !== undefined && (
        <Image
          className="detail__map"
          src={sanPham.mapImage}
          // Bản đồ cũng là ảnh có chữ: font của nó phải dựng được cả `æ ø å`
          // lẫn dấu tiếng Việt — bản demo đã dính bẫy này một lần.
          alt={sanPham.heroImageAlt}
          width={1200}
          height={800}
          unoptimized
        />
      )}
    </article>
  );
}

/* ------------------------------------------------------------ R6 điểm đến */

/**
 * Trang một điểm đến (R6): giới thiệu, rồi danh sách tour tới đó.
 *
 * Danh sách tour lọc bằng tham số `destination` của endpoint sản phẩm — cùng
 * một endpoint mà trang danh sách dùng, chỉ khác bộ lọc. Không có endpoint
 * riêng "tour theo điểm đến", và cũng không cần: thêm một đường đọc thứ hai cho
 * cùng dữ liệu là thêm một chỗ để quên điều kiện `NOT soft_delete`.
 */
async function DestinationDetailPage({ locale, slug }: { locale: Locale; slug: string }) {
  const diemDen = await layDiemDen(locale, slug);

  return (
    <article className="detail">
      <p className="detail__breadcrumb">
        <Link href={duongDanDiemDen(locale)}>{t(locale, 'destinations.heading')}</Link>
      </p>

      <h1>{diemDen.name}</h1>
      <p className="detail__meta">{diemDen.region.name}</p>
      {diemDen.summary && <p>{diemDen.summary}</p>}

      <h2>{t(locale, 'destinations.viewProducts')}</h2>
      <Suspense fallback={<div className="skeleton-bar" />}>
        <TourTaiDiemDen locale={locale} slug={slug} />
      </Suspense>
    </article>
  );
}

async function TourTaiDiemDen({ locale, slug }: { locale: Locale; slug: string }) {
  const { market } = await resolveMarket(locale);

  let trang;
  try {
    trang = await productsApi().listProducts({
      ...requestScope(market, locale),
      destination: slug,
      size: 12,
    });
  } catch (loi) {
    console.error('[api] listProducts theo điểm đến thất bại', loi);
    return (
      <div className="state state--error" role="alert">
        <p className="state__title">{t(locale, 'state.error.title')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  // Rỗng KÈM hướng dẫn hành động, không phải một dòng "không có gì" cụt lủn
  // (web/CLAUDE.md mục 5.3): điểm đến chưa có tour vẫn là khách quan tâm tới nơi
  // đó, và đó là lúc đáng mời họ gọi điện nhất.
  if (trang.items.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'destinations.noProducts')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  return (
    <ul className="product-grid">
      {trang.items.map((sp) => (
        <ProductCard key={sp.slug} locale={locale} product={sp} />
      ))}
    </ul>
  );
}

/* ------------------------------------------------------------ R7 đặt tour */

/**
 * Đặt tour (R7).
 *
 * Máy chủ nạp sản phẩm và lịch khởi hành; bốn bước chạy ở trình duyệt vì mỗi lần
 * khách đổi lựa chọn là một lần gọi lại endpoint tính giá.
 *
 * `PRIVATE_TOUR` **không đặt trực tiếp được** (docs/04): CTA của nó là yêu cầu
 * báo giá. Vào thẳng URL đặt tour của một `PRIVATE_TOUR` thì trả về trang chi
 * tiết, nơi có đúng nút cần bấm.
 */
async function DatTourPage({
  locale,
  slug,
  thamSo,
}: {
  locale: Locale;
  slug: string;
  thamSo: Record<string, string | string[] | undefined>;
}) {
  const { market } = await resolveMarket(locale);
  const sanPham = await laySanPham(locale, slug);

  if (sanPham.productType === 'PRIVATE_TOUR') {
    permanentRedirect(duongDanChiTiet(locale, slug));
  }

  let ngayKhoiHanh: Departure[];
  try {
    ngayKhoiHanh = await productsApi().listDepartures({
      ...requestScope(market, locale),
      slug,
    });
  } catch (loi) {
    console.error('[api] listDepartures thất bại', loi);
    ngayKhoiHanh = [];
  }

  return (
    <>
      <p className="detail__breadcrumb">
        <Link href={duongDanChiTiet(locale, slug)}>{t(locale, 'detail.backToList')}</Link>
      </p>
      <DatTour
        locale={locale}
        market={market}
        slug={slug}
        tieuDe={sanPham.title}
        ngayKhoiHanh={ngayKhoiHanh}
        trangThai={docTrangThai(thamSo)}
      />
    </>
  );
}

/**
 * Đọc trạng thái ba bước đầu từ tham số truy vấn.
 *
 * Giá trị lạ thì lùi về bước 1 chứ không nổ: URL do khách gõ tay hoặc dán từ chỗ
 * khác là chuyện bình thường, và một trang lỗi ở giữa luồng đặt tour là một đơn
 * mất.
 */
function docTrangThai(thamSo: Record<string, string | string[] | undefined>): TrangThai {
  const mot = (ten: string) => (typeof thamSo[ten] === 'string' ? thamSo[ten] : undefined);

  const buoc = Number(mot('buoc') ?? '1');
  const ngay = mot('ngay');

  const pax = (mot('khach') ?? '')
    .split(',')
    .map((phan) => phan.split(':'))
    .filter((c): c is [string, string] => c.length === 2 && Number.isFinite(Number(c[1])))
    .map(([paxTypeCode, count]) => ({ paxTypeCode, count: Number(count) }));

  return {
    // Không có ngày khởi hành thì mọi bước sau đều vô nghĩa — về bước 1.
    buoc: !ngay || buoc < 1 || buoc > 4 ? 1 : ((buoc as 1 | 2 | 3 | 4) ?? 1),
    departureId: ngay,
    holdId: mot('giu'),
    hetHan: mot('het'),
    // Loại khách mặc định: một người lớn. Ô đếm phải có sẵn một dòng để bấm.
    pax: pax.length > 0 ? pax : [{ paxTypeCode: 'ADULT', count: 1 }],
    singleTravellers: Number(mot('mot') ?? '0') || undefined,
  };
}

/* ------------------------------------------------------------ R8 xác nhận */

/**
 * Trang xác nhận (R8).
 *
 * Không có đăng nhập cho khách ở v1, nên tra cứu bằng **mã tra cứu + email
 * khớp** (docs/23 mục 6). Thiếu email thì không tra được — và đó là chủ ý, không
 * phải thiếu sót: mã tra cứu một mình là một kênh dò đơn của người khác.
 */
async function XacNhanPage({
  locale,
  reference,
  thamSo,
}: {
  locale: Locale;
  reference: string;
  thamSo: Record<string, string | string[] | undefined>;
}) {
  const email = typeof thamSo.email === 'string' ? thamSo.email : '';
  const { market } = await resolveMarket(locale);

  let don;
  if (email !== '') {
    try {
      don = await bookingApi().traDon({ ...requestScope(market, locale), reference, email });
    } catch (loi) {
      if (!laKhongTimThay(loi)) {
        throw loi;
      }
    }
  }

  if (!don) {
    return (
      <section className="detail">
        <h1>{t(locale, 'confirm.heading')}</h1>
        <div className="state state--error" role="alert">
          <p className="state__title">{t(locale, 'confirm.notFound')}</p>
          <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
        </div>
      </section>
    );
  }

  return (
    <section className="detail">
      <h1>{t(locale, 'confirm.heading')}</h1>

      <p className="dat-ma">
        {t(locale, 'confirm.reference')}: <strong>{don.reference}</strong>
      </p>
      <p className="phu">{t(locale, 'confirm.keepIt')}</p>

      <h2>{don.productTitle}</h2>
      {don.departDate && (
        <p className="detail__meta">
          {new Intl.DateTimeFormat(locale === 'da' ? 'da-DK' : 'vi-VN', {
            dateStyle: 'long',
          }).format(don.departDate)}
        </p>
      )}

      <table className="dat-gia">
        <tbody>
          <tr>
            <th scope="row">{t(locale, 'booking.total')}</th>
            <td>{formatMoney(don.total, locale)}</td>
          </tr>
          <tr>
            <th scope="row">{t(locale, 'booking.deposit')}</th>
            <td>{formatMoney(don.deposit, locale)}</td>
          </tr>
          <tr>
            <th scope="row">{t(locale, 'booking.balance')}</th>
            <td>{formatMoney(don.balance, locale)}</td>
          </tr>
        </tbody>
      </table>

      <p>{t(locale, 'confirm.next')}</p>
    </section>
  );
}
