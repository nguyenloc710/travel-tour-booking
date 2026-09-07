import { NextResponse, type NextRequest } from 'next/server';
import { defaultMarketFor, isLocale, isMarket, locales, SOURCE_LOCALE } from '@travel/i18n';

/** Cookie market sống một năm — docs/02 mục 5.3. */
const MOT_NAM_GIAY = 60 * 60 * 24 * 365;

/**
 * Locale ở URL, market ở cookie.
 *
 * Next 16 đổi tên quy ước `middleware` thành `proxy` — cùng cơ chế, chạy trước
 * mọi request khớp `matcher`.
 *
 * Market **không** nằm trong URL: URL canonical chỉ chứa locale nên không sinh
 * nội dung trùng lặp với công cụ tìm kiếm. Cookie chỉ ghi đè market mặc định
 * của locale, và khi khác mặc định thì giao diện hiện băng thông báo thường
 * trực — người dùng không được phép nhầm giá.
 */
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const doanDau = pathname.split('/')[1];

  if (!isLocale(doanDau)) {
    const url = request.nextUrl.clone();
    url.pathname = `/${chonLocale(request)}${pathname === '/' ? '' : pathname}`;
    return NextResponse.redirect(url);
  }

  const phanHoi = NextResponse.next();

  // Chưa chọn thị trường thì lấy mặc định của locale. Ghi cookie để lần sau
  // server render biết ngay, không phải đoán lại.
  const dangCo = request.cookies.get('market')?.value;
  if (!isMarket(dangCo)) {
    phanHoi.cookies.set('market', defaultMarketFor(doanDau), {
      maxAge: MOT_NAM_GIAY,
      sameSite: 'lax',
      path: '/',
    });
  }

  return phanHoi;
}

/**
 * Chọn locale cho khách chưa có trong URL.
 *
 * Đây là chuỗi giao diện chứ không phải nội dung bán hàng, nên fallback về
 * ngôn ngữ nguồn `da` là đúng chính sách.
 */
function chonLocale(request: NextRequest): string {
  const header = request.headers.get('accept-language') ?? '';
  for (const phan of header.split(',')) {
    const the = phan.split(';')[0]?.trim().toLowerCase().split('-')[0];
    if (isLocale(the)) return the;
  }
  return SOURCE_LOCALE;
}

export const config = {
  matcher: ['/((?!_next|api|favicon.ico|.*\\..*).*)'],
};

export { locales };
