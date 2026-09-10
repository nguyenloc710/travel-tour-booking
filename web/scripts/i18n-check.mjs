#!/usr/bin/env node
/**
 * Độ phủ chuỗi giao diện. Thiếu khoá là **LỖI**, không phải cảnh báo.
 *
 * Đây là chỗ dự án đa ngôn ngữ hỏng âm thầm nhất: thiếu bản dịch không làm gãy
 * build, chỉ làm khách Đan Mạch nhìn thấy tiếng Việt trên trang thanh toán.
 * Fallback lúc chạy là lưới an toàn; bộ kiểm này mới là hàng rào.
 *
 *     node scripts/i18n-check.mjs
 *
 * Mã thoát: 0 đủ · 1 thiếu.
 */
import { readFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';

const GOC = join(dirname(fileURLToPath(import.meta.url)), '..');
const THU_MUC = join(GOC, 'packages', 'i18n', 'messages');
const NGON_NGU_NGUON = 'da';

function doc(locale) {
  return JSON.parse(readFileSync(join(THU_MUC, `${locale}.json`), 'utf8'));
}

const locales = readdirSync(THU_MUC)
  .filter((f) => f.endsWith('.json'))
  .map((f) => f.replace('.json', ''));

if (!locales.includes(NGON_NGU_NGUON)) {
  console.error(`Không tìm thấy catalog của ngôn ngữ nguồn "${NGON_NGU_NGUON}".`);
  process.exit(1);
}

const nguon = doc(NGON_NGU_NGUON);
const khoaNguon = Object.keys(nguon).sort();
let soLoi = 0;

console.log(`Ngôn ngữ nguồn: ${NGON_NGU_NGUON} — ${khoaNguon.length} khoá\n`);

for (const locale of locales) {
  if (locale === NGON_NGU_NGUON) continue;

  const catalog = doc(locale);
  const khoa = Object.keys(catalog);

  const thieu = khoaNguon.filter((k) => !(k in catalog));
  const thua = khoa.filter((k) => !(k in nguon)).sort();
  const rong = khoa.filter((k) => typeof catalog[k] === 'string' && catalog[k].trim() === '');

  // Tham số {ten} phải khớp giữa hai bản, nếu không chuỗi dịch mất chỗ thay thế.
  const lechThamSo = khoaNguon
    .filter((k) => k in catalog)
    .map((k) => ({ k, a: thamSo(nguon[k]), b: thamSo(catalog[k]) }))
    .filter(({ a, b }) => a.join(',') !== b.join(','));

  const dat = thieu.length === 0 && thua.length === 0 && rong.length === 0 && lechThamSo.length === 0;
  const phuTram = (((khoaNguon.length - thieu.length) / khoaNguon.length) * 100).toFixed(1);

  console.log(`${locale}: ${phuTram}% — ${dat ? 'ĐẠT' : 'LỖI'}`);

  for (const k of thieu) {
    console.log(`  thiếu khoá        ${k}`);
    soLoi++;
  }
  for (const k of thua) {
    console.log(`  khoá thừa         ${k}  (không có ở ${NGON_NGU_NGUON} — khoá chết)`);
    soLoi++;
  }
  for (const k of rong) {
    console.log(`  chuỗi rỗng        ${k}`);
    soLoi++;
  }
  for (const { k, a, b } of lechThamSo) {
    console.log(`  lệch tham số      ${k}  (${NGON_NGU_NGUON}: {${a}} · ${locale}: {${b}})`);
    soLoi++;
  }
  console.log('');
}

function thamSo(chuoi) {
  return [...String(chuoi).matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort();
}

// ------------------------------------------------ mã lỗi ↔ khoá `error.*`
//
// Danh mục mã lỗi là enum `ErrorCode` trong hợp đồng (docs/13 mục 5.1). Bảng
// dịch của website khách phải nói về đúng danh mục đó.
//
// Hai chiều KHÔNG đối xứng, và đó là chủ ý:
//
//   · khoá `error.X` mà `X` không có trong enum → **LỖI**. Đó là khoá chết:
//     không mã nào phát ra nó, nên câu dịch ấy không bao giờ hiện. Trước khi có
//     phép kiểm này, `error.MARKET_NOT_FOUND` đã nằm đó và chưa ai để ý.
//
//   · mã trong enum mà không có khoá → **không phải lỗi**. Mã chưa có câu riêng
//     thì rơi về `error.generic`, đúng như web/CLAUDE.md mục 4 đã chốt: không
//     hiện mã ra khách. Phần lớn danh mục phục vụ bề mặt quản trị và không bao
//     giờ tới mắt khách, nên bắt buộc dịch đủ là ép viết câu cho thứ không xảy
//     ra. Vẫn in ra một dòng để không ai quên một mã KHÁCH gặp thật.

const maHopDong = new Set(
  parse(readFileSync(join(GOC, '..', 'contracts', 'openapi.yaml'), 'utf8'))
    ?.components?.schemas?.ErrorCode?.enum ?? [],
);

if (maHopDong.size === 0) {
  console.error('Không đọc được enum ErrorCode trong contracts/openapi.yaml.');
  process.exit(1);
}

const maCoCau = khoaNguon
  .filter((k) => k.startsWith('error.') && k !== 'error.generic')
  .map((k) => k.slice('error.'.length));

const khoaChet = maCoCau.filter((m) => !maHopDong.has(m));
const chuaCoCau = [...maHopDong].filter((m) => !maCoCau.includes(m)).sort();

console.log(`Mã lỗi: ${maHopDong.size} trong hợp đồng · ${maCoCau.length} có câu riêng`);
for (const m of khoaChet) {
  console.log(`  khoá chết         error.${m}  (không có trong enum ErrorCode — không mã nào phát ra)`);
  soLoi++;
}
if (chuaCoCau.length > 0) {
  console.log(`  ${chuaCoCau.length} mã dùng câu chung: ${chuaCoCau.join(' ')}`);
}
console.log('');

if (soLoi > 0) {
  console.error(`${soLoi} lỗi. Thiếu khoá dịch là LỖI, không phải cảnh báo — web/CLAUDE.md mục 2.`);
  process.exit(1);
}

console.log('Độ phủ chuỗi giao diện: ĐẠT');
