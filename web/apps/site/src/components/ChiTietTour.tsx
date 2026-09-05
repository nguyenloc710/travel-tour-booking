import { Suspense } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { t, type Locale, type Market } from '@travel/i18n';
import { formatDate, formatNumber, PriceFrom } from '@travel/ui';
import type { Departure, HotelStay, ItineraryDay, ProductDetail } from '@travel/api-client';
import { productsApi, requestScope } from '@/lib/api';
import { duongDanDatTour, duongDanListing, productsSegment } from '@/lib/routes';
import { duongDanTab, tabCoMat, type TabKey } from '@/lib/tabs';
import { ProductCard } from '@/components/ProductCard';
import { ProductFacts } from '@/components/ProductFacts';
import { SaoDanhGia } from '@/components/SaoDanhGia';
import { YeuCauBaoGia } from '@/components/YeuCauBaoGia';

/**
 * Trang chi tiết sản phẩm, chia theo **tab** — `docs/05` mục 3.
 *
 * Trước bản này nó là một trang cuộn phẳng: mọi thứ đổ xuống một cột, và khách
 * muốn xem ngày khởi hành phải cuộn qua toàn bộ mô tả. `docs/05` mô tả một trang
 * có tab từ đầu, và lý do không phải thẩm mỹ — khách quyết định mua sau khi so
 * ngày và giá, nên đường tới bảng đó phải là một cú bấm.
 *
 * **Tab nằm trong URL, và tab là liên kết neo.** Ba hệ quả, cả ba đều là yêu cầu
 * chứ không phải tiện tay:
 *
 * - F5 giữ nguyên tab, và khách dán link cho nhau ra đúng tab đó.
 * - Trang chạy được khi JavaScript chưa tải xong — mỗi tab là một lần tải trang.
 * - Mỗi tab **chỉ nạp dữ liệu của nó**. Tải hết bốn khối rồi ẩn ba là bắt khách
 *   chờ ba lần gọi API họ không xem.
 *
 * **Thanh dính đáy** (`docs/05` mục 4) đi theo suốt trang: tên tour, giá "từ" và
 * nút chính. `PRIVATE_TOUR` không có nút "Đặt tour" ở bất kỳ đâu — đặt nhầm nút
 * ở đó là lỗi nghiệp vụ, không phải lỗi chữ.
 */
export async function ChiTietTour({
  sanPham,
  locale,
  market,
  slug,
  tab,
}: {
  sanPham: ProductDetail;
  locale: Locale;
  market: Market;
  slug: string;
  tab: TabKey;
}) {
  const tabs = tabCoMat(sanPham.productType);
  // Tab không có mặt ở loại này thì về tổng quan thay vì hiện một khối rỗng.
  const dangXem: TabKey = tabs.includes(tab) ? tab : 'tongQuan';
  const doan = productsSegment(locale);

  return (
    <article className="detail">
      {/* Ảnh nằm SAU chữ chứ không nằm trên nó: tiêu đề là thứ khách cần đọc
          đầu tiên, và một tấm ảnh cao 640px đẩy nó xuống dưới nếp gấp. Chữ đọc
          được nhờ vùng tối ở đáy ảnh, vốn nằm sẵn trong chính tệp ảnh. */}
      <header className="chi-tiet-mo-dau">
        <Image
          className="chi-tiet-mo-dau__anh"
          src={sanPham.heroImage}
          alt={sanPham.heroImageAlt}
          width={1600}
          height={900}
          priority
          unoptimized
        />

        <div className="chi-tiet-mo-dau__chu">
          <p className="chi-tiet-mo-dau__duong">
            <Link href={duongDanListing(locale)}>{t(locale, 'detail.backToList')}</Link>
          </p>

          <p className="chi-tiet-mo-dau__nhan">
            <span className="badge">{t(locale, `productType.${sanPham.productType}`)}</span>
            {sanPham.isNew && <span className="badge badge--new">{t(locale, 'products.isNew')}</span>}
          </p>

          <h1>{sanPham.title}</h1>
          <p className="chi-tiet-mo-dau__tom-tat">{sanPham.shortDescription}</p>
        </div>
      </header>

      {/* Bốn dữ kiện quyết định, đọc được trong một cái liếc. Chúng lặp lại thứ
          đã có ở chỗ khác trên trang, và lặp là chủ ý: khách so hai tour cạnh
          nhau bằng đúng bốn con số này. */}
      <dl className="so-lieu">
        <div>
          <dt>{t(locale, 'detail.factbar.type')}</dt>
          <dd>{t(locale, `productType.${sanPham.productType}`)}</dd>
        </div>
        {sanPham.durationDays !== undefined && (
          <div>
            <dt>{t(locale, 'detail.factbar.duration')}</dt>
            <dd>
              {t(locale, 'products.duration', {
                days: formatNumber(sanPham.durationDays, locale),
              })}
            </dd>
          </div>
        )}
        <div>
          <dt>{t(locale, 'detail.factbar.where')}</dt>
          <dd>
            {sanPham.destination.name} · {sanPham.region.name}
          </dd>
        </div>
        {sanPham.rating !== undefined && (
          <div>
            <dt>{t(locale, 'detail.factbar.rating')}</dt>
            <dd>
              <SaoDanhGia
                rating={sanPham.rating}
                reviewCount={sanPham.reviewCount}
                locale={locale}
              />
            </dd>
          </div>
        )}
      </dl>

      {/* Thanh tab DÍNH dưới đầu trang: ở tab lịch trình của tour 16 ngày, khách
          cuộn xuống ngày thứ mười rồi muốn sang bảng giá — không có thanh dính
          thì phải cuộn ngược hết lên. */}
      <nav className="tab-thanh" aria-label={t(locale, 'detail.tabs')}>
        <div className="tab-thanh__trong">
          {tabs.map((k) => (
            <Link
              key={k}
              href={duongDanTab(locale, doan, slug, k)}
              aria-current={k === dangXem ? 'page' : undefined}
            >
              {t(locale, `detail.tab.${k}`)}
            </Link>
          ))}
        </div>
      </nav>

      {dangXem === 'tongQuan' && (
        <TabTongQuan sanPham={sanPham} locale={locale} market={market} slug={slug} />
      )}
      {dangXem === 'lichTrinh' && (
        <Suspense fallback={<KhoiCho />}>
          <TabLichTrinh locale={locale} market={market} slug={slug} sanPham={sanPham} />
        </Suspense>
      )}
      {dangXem === 'khachSan' && (
        <Suspense fallback={<KhoiCho />}>
          <TabKhachSan locale={locale} market={market} slug={slug} />
        </Suspense>
      )}
      {dangXem === 'giaVaNgay' && (
        <Suspense fallback={<KhoiCho />}>
          <TabGiaVaNgay locale={locale} market={market} slug={slug} />
        </Suspense>
      )}
      {dangXem === 'thongTinThucTe' && <ProductFacts product={sanPham} locale={locale} />}

      <ThanhDay sanPham={sanPham} locale={locale} slug={slug} doan={doan} />
    </article>
  );
}

/* --------------------------------------------------------- 1. Tổng quan */

/**
 * Thứ tự khối lấy từ `docs/05` mục 5.1, và giữ nguyên vì nó đi từ **cảm hứng**
 * tới **chi tiết** tới **hành động**.
 *
 * Bốn khối của tài liệu chưa dựng được: bộ ảnh và bản đồ lớn dạng lightbox
 * (chờ media, **Q-6**), hồ sơ nhân viên tư vấn (chưa có endpoint), trích dẫn
 * khách và rating (chưa có bảng). Bỏ trống còn hơn dựng ô giả — một khối rỗng
 * trông như đã xong.
 */
async function TabTongQuan({
  sanPham,
  locale,
  market,
  slug,
}: {
  sanPham: ProductDetail;
  locale: Locale;
  market: Market;
  slug: string;
}) {
  return (
    <>
      {/* `PRIVATE_TOUR`: khối "chuyến đi này điều chỉnh được" đứng NGAY sau đoạn
          mở đầu (docs/05 mục 5.1). Nó là điểm bán của loại này, và để xuống dưới
          thì khách đã bỏ trang trước khi đọc tới. */}
      {sanPham.productType === 'PRIVATE_TOUR' && (
        <p className="state cot-chu">
          <strong className="state__title">{t(locale, 'detail.private.flexible.title')}</strong>
          {t(locale, 'detail.private.flexible.body')}
        </p>
      )}

      <section className="why">
        <h2>{t(locale, 'detail.whyChooseThis')}</h2>
        <ul>
          {sanPham.whyChooseThis.map((ly_do) => (
            <li key={ly_do}>{ly_do}</li>
          ))}
        </ul>
      </section>

      <div className="detail__body">
        {/* Từng đoạn văn một, không phải một khối HTML: nội dung do biên tập
            viên nhập, và HTML tự do từ CSDL là lỗ chèn mã chờ sẵn. */}
        {sanPham.longDescription.map((doan_van, i) => (
          <p key={i}>{doan_van}</p>
        ))}
      </div>

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

      {/* Tour riêng không đặt trực tiếp được, nên form báo giá LÀ điểm cuối của
          trang thay cho nút "Đặt tour" (docs/04). */}
      {sanPham.productType === 'PRIVATE_TOUR' && (
        <YeuCauBaoGia
          locale={locale}
          market={market}
          slug={slug}
          leadTimeDays={sanPham.leadTimeDays}
        />
      )}

      <Suspense fallback={null}>
        <TourLienQuan locale={locale} market={market} sanPham={sanPham} slug={slug} />
      </Suspense>
    </>
  );
}

/**
 * Sản phẩm liên quan cùng điểm đến — khối 9 của `docs/05` mục 5.1.
 *
 * Lọc bằng chính endpoint danh sách, không có endpoint riêng: thêm một đường đọc
 * thứ hai cho cùng dữ liệu là thêm một chỗ để quên điều kiện `NOT soft_delete`.
 *
 * Không hiện gì khi chỉ có chính nó — một khối "tour liên quan" chứa đúng tour
 * đang xem là một khối làm khách bấm rồi quay lại chỗ cũ.
 */
async function TourLienQuan({
  locale,
  market,
  sanPham,
  slug,
}: {
  locale: Locale;
  market: Market;
  sanPham: ProductDetail;
  slug: string;
}) {
  let khac;
  try {
    const trang = await productsApi().listProducts({
      ...requestScope(market, locale),
      destination: sanPham.destination.slug,
      size: 4,
    });
    khac = trang.items.filter((sp) => sp.slug !== slug).slice(0, 3);
  } catch (loi) {
    // Khối phụ hỏng thì im lặng bỏ qua: nó không phải lý do khách vào trang, và
    // một băng lỗi ở đây làm hỏng cả trang chi tiết đang hiện đúng.
    console.error('[api] listProducts (liên quan) thất bại', loi);
    return null;
  }

  if (khac.length === 0) {
    return null;
  }

  return (
    <section>
      <h2>{t(locale, 'detail.related', { destination: sanPham.destination.name })}</h2>
      <ul className="product-grid">
        {khac.map((sp) => (
          <li key={sp.slug}>
            <ProductCard product={sp} locale={locale} />
          </li>
        ))}
      </ul>
    </section>
  );
}

/* -------------------------------------------------------- 2. Lịch trình */

async function TabLichTrinh({
  locale,
  market,
  slug,
  sanPham,
}: {
  locale: Locale;
  market: Market;
  slug: string;
  sanPham: ProductDetail;
}) {
  let ngay: ItineraryDay[];
  try {
    ngay = await productsApi().getItinerary({ ...requestScope(market, locale), slug });
  } catch (loi) {
    console.error('[api] listItinerary thất bại', loi);
    return <TrangThaiLoi locale={locale} />;
  }

  if (ngay.length === 0) {
    return <TrangThaiRong locale={locale} />;
  }

  return (
    <section>
      <h2>{t(locale, 'detail.tab.lichTrinh')}</h2>

      {/* Băng thông báo THƯỜNG TRỰC cho tour riêng — `docs/05` mục 5.2. Thiếu
          nó là hứa sai với khách: đây là lộ trình mẫu, không phải lịch cố định. */}
      {sanPham.productType === 'PRIVATE_TOUR' && (
        <p className="state cot-chu">{t(locale, 'detail.itinerary.sample')}</p>
      )}

      <ol className="lich-trinh">
        {ngay.map((d) => (
          <li key={d.dayNumber} className="lich-trinh__ngay">
            <p className="lich-trinh__so">
              {t(locale, 'detail.itinerary.day', { n: formatNumber(d.dayNumber, locale) })}
            </p>
            <h3>{d.title}</h3>
            <p>{d.description}</p>

            <p className="lich-trinh__chi-tiet">
              {d.destination !== undefined && (
                <span>
                  {t(locale, 'detail.itinerary.overnight')}: {d.destination.name}
                </span>
              )}
              {/* Đêm trên tàu hoả hoặc máy bay: không hiện khách sạn, hiện nhãn
                  "ngủ trên tàu" (`docs/05` mục 5.2). */}
              <span>
                {d.hotelName !== undefined
                  ? d.hotelName
                  : t(locale, 'detail.itinerary.enRoute')}
              </span>
            </p>
          </li>
        ))}
      </ol>
    </section>
  );
}

/* -------------------------------------------------------- 3. Khách sạn */

async function TabKhachSan({
  locale,
  market,
  slug,
}: {
  locale: Locale;
  market: Market;
  slug: string;
}) {
  let chang: HotelStay[];
  try {
    chang = await productsApi().getHotelStays({ ...requestScope(market, locale), slug });
  } catch (loi) {
    console.error('[api] listHotels thất bại', loi);
    return <TrangThaiLoi locale={locale} />;
  }

  if (chang.length === 0) {
    return <TrangThaiRong locale={locale} />;
  }

  // Tổng số đêm hiện ở ĐẦU tab — `docs/05` mục 5.3.
  const tongDem = chang.reduce((t_, c) => t_ + c.nights, 0);

  return (
    <section>
      <h2>{t(locale, 'detail.tab.khachSan')}</h2>
      <p className="listing__total">
        {t(locale, 'detail.hotels.totalNights', { nights: formatNumber(tongDem, locale) })}
      </p>

      <ul className="khach-san">
        {chang.map((c) => (
          <li key={`${c.name}-${c.destination.slug}`} className="khach-san__the">
            {/* Ảnh là trường TUỲ CHỌN: một khách sạn chưa có ảnh vẫn hiện được,
                và thẻ phải xếp đúng khi thiếu ảnh chứ không để lại một ô trống. */}
            {c.image !== undefined && (
              <Image
                className="khach-san__anh"
                src={c.image}
                alt={t(locale, 'detail.hotels.imageAlt', { name: c.name })}
                width={640}
                height={428}
                unoptimized
              />
            )}

            <div className="khach-san__than">
              <p className="khach-san__noi">{c.destination.name}</p>
              {/* Tên riêng của khách sạn KHÔNG dịch — `docs/24` mục 5. */}
              <h3>{c.name}</h3>

              <p className="khach-san__dong">
                <span className="badge">
                  {t(locale, c.nights === 1 ? 'detail.hotels.night' : 'detail.hotels.nights', {
                    count: formatNumber(c.nights, locale),
                  })}
                </span>
                {/* Số sao hiện bằng CHỮ kèm số, không chỉ bằng biểu tượng: màu
                    và hình không bao giờ là phương tiện duy nhất (docs/21 mục 2). */}
                {c.stars !== undefined && (
                  <span className="badge">
                    {t(locale, 'detail.hotels.stars', { count: formatNumber(c.stars, locale) })}
                  </span>
                )}
              </p>

              {c.description !== undefined && <p>{c.description}</p>}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

/* ------------------------------------------------------- 4. Giá và ngày */

async function TabGiaVaNgay({
  locale,
  market,
  slug,
}: {
  locale: Locale;
  market: Market;
  slug: string;
}) {
  let ngay: Departure[];
  try {
    ngay = await productsApi().listDepartures({ ...requestScope(market, locale), slug });
  } catch (loi) {
    console.error('[api] listDepartures thất bại', loi);
    return <TrangThaiLoi locale={locale} />;
  }

  if (ngay.length === 0) {
    return (
      <div className="state state--empty">
        <p className="state__title">{t(locale, 'booking.noDepartures')}</p>
        <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
      </div>
    );
  }

  return (
    <section>
      <h2>{t(locale, 'detail.tab.giaVaNgay')}</h2>

      {/* Ghi chú giá ở ĐẦU bảng là yêu cầu pháp lý — `docs/05` mục 5.5. */}
      <p className="listing__total cot-chu">{t(locale, 'detail.departures.priceNote')}</p>

      <div className="bang-khung">
        <table className="bang">
          <thead>
            <tr>
              <th>{t(locale, 'detail.departures.departDate')}</th>
              <th>{t(locale, 'detail.departures.returnDate')}</th>
              <th>{t(locale, 'detail.departures.days')}</th>
              <th>{t(locale, 'detail.departures.price')}</th>
              <th>{t(locale, 'detail.departures.status')}</th>
              <th>{t(locale, 'detail.departures.book')}</th>
            </tr>
          </thead>
          <tbody>
            {ngay.map((d) => (
              <tr key={d.id}>
                <td>{formatDate(d.departDate, locale)}</td>
                <td>{formatDate(d.returnDate, locale)}</td>
                <td>{formatNumber(d.days, locale)}</td>
                <td>{d.priceFrom ? <PriceFrom price={d.priceFrom} locale={locale} /> : '—'}</td>
                {/* Trạng thái hiện bằng CHỮ, không chỉ bằng màu — ngày hết chỗ
                    phải đọc được là "hết chỗ" (docs/21 mục 2 quy tắc 2). */}
                <td>
                  {/* Lớp theo trạng thái CHỈ đổi màu; chữ vẫn nói đủ nghĩa nếu
                      màu không tới được mắt người đọc (docs/21 mục 2 quy tắc 2). */}
                  <span className={`badge badge--tt badge--tt-${d.status}`}>
                    {t(locale, `departureStatus.${d.status}`)}
                  </span>
                  <br />
                  <span className="product-card__where">
                    {t(
                      locale,
                      d.seatsAvailable === 1 ? 'booking.seatsLeft.one' : 'booking.seatsLeft.many',
                      { count: formatNumber(d.seatsAvailable, locale) },
                    )}
                  </span>
                </td>
                <td>
                  {d.status === 'OPEN' || d.status === 'FEW_SEATS' || d.status === 'GUARANTEED' ? (
                    <Link
                      className="nut"
                      href={duongDanDatTour(
                        locale,
                        slug,
                        new URLSearchParams({ buoc: '2', departureId: d.id }),
                      )}
                    >
                      {t(locale, 'detail.departures.book')}
                    </Link>
                  ) : (
                    <span className="product-card__where">{t(locale, 'booking.closed')}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

/* ------------------------------------------------------ Thanh dính đáy */

/**
 * `docs/05` mục 4.
 *
 * Nút chính khác nhau theo loại, và khác biệt đó là **nghiệp vụ**:
 * `PRIVATE_TOUR` dẫn tới form báo giá, mọi loại khác dẫn tới tab giá và ngày.
 * Loại này không có nút "Đặt tour" ở bất kỳ đâu trên trang.
 */
function ThanhDay({
  sanPham,
  locale,
  slug,
  doan,
}: {
  sanPham: ProductDetail;
  locale: Locale;
  slug: string;
  doan: string;
}) {
  const laTourRieng = sanPham.productType === 'PRIVATE_TOUR';

  return (
    <div className="thanh-day">
      <p className="thanh-day__ten">{sanPham.title}</p>

      <div className="thanh-day__gia">
        {sanPham.priceFrom ? (
          <PriceFrom price={sanPham.priceFrom} locale={locale} />
        ) : (
          <span>{t(locale, 'products.contactForPrice')}</span>
        )}
      </div>

      <Link
        className="nut"
        href={
          laTourRieng
            ? duongDanTab(locale, doan, slug, 'tongQuan', '#bao-gia')
            : duongDanTab(locale, doan, slug, 'giaVaNgay')
        }
      >
        {laTourRieng ? t(locale, 'detail.private.cta') : t(locale, 'detail.bookCta')}
      </Link>
    </div>
  );
}

/* ------------------------------------------------------------ dùng chung */

function KhoiCho() {
  return (
    <div aria-hidden="true">
      <span className="skeleton-bar" />
      <span className="skeleton-bar" />
      <span className="skeleton-bar skeleton-bar--short" />
    </div>
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

function TrangThaiRong({ locale }: { locale: Locale }) {
  return (
    <div className="state state--empty">
      <p className="state__title">{t(locale, 'state.empty.title')}</p>
      <p>{t(locale, 'state.error.help', { phone: t(locale, 'site.phone') })}</p>
    </div>
  );
}
