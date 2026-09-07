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

if (soLoi > 0) {
  console.error(`${soLoi} lỗi. Thiếu khoá dịch là LỖI, không phải cảnh báo — web/CLAUDE.md mục 2.`);
  process.exit(1);
}

console.log('Độ phủ chuỗi giao diện: ĐẠT');
