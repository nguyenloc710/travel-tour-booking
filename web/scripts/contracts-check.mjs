#!/usr/bin/env node
/**
 * Kiểm tương thích ngược của `contracts/openapi.yaml` so với nhánh chính.
 *
 * Đổi phá vỡ tương thích không phải là điều cấm — nó là điều phải **biết trước
 * khi mở pull request**, và phải lên phiên bản đường dẫn `/api/v2/` chứ không
 * sửa lặng lẽ tại chỗ (docs/13).
 *
 *     pnpm contracts:check [nhánh-gốc]      mặc định: main
 *
 * Mã thoát: 0 tương thích · 1 có thay đổi phá vỡ.
 */
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';

const GOC = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const DUONG_DAN = 'contracts/openapi.yaml';
const nhanhGoc = process.argv[2] ?? 'main';

const banCu = docTuGit(`${nhanhGoc}:${DUONG_DAN}`);
if (banCu === null) {
  console.log(`Chưa có ${DUONG_DAN} ở nhánh ${nhanhGoc} — coi như spec mới, bỏ qua kiểm.`);
  process.exit(0);
}

const cu = parse(banCu);
const moi = parse(readFileSync(join(GOC, DUONG_DAN), 'utf8'));

const pha = [];
const them = [];

// --- endpoint bị xoá hoặc đổi tên
for (const [duongDan, thaoTacCu] of Object.entries(cu.paths ?? {})) {
  const thaoTacMoi = moi.paths?.[duongDan];
  if (!thaoTacMoi) {
    pha.push(`xoá đường dẫn  ${duongDan}`);
    continue;
  }
  for (const phuongThuc of Object.keys(thaoTacCu)) {
    if (!thaoTacMoi[phuongThuc]) {
      pha.push(`xoá thao tác   ${phuongThuc.toUpperCase()} ${duongDan}`);
      continue;
    }
    soSanhThamSo(duongDan, phuongThuc, thaoTacCu[phuongThuc], thaoTacMoi[phuongThuc]);
  }
}

for (const duongDan of Object.keys(moi.paths ?? {})) {
  if (!cu.paths?.[duongDan]) them.push(`thêm đường dẫn ${duongDan}`);
}

// --- schema: xoá trường, hoặc thêm trường bắt buộc
const schemaCu = cu.components?.schemas ?? {};
const schemaMoi = moi.components?.schemas ?? {};

for (const [ten, cuSchema] of Object.entries(schemaCu)) {
  const moiSchema = schemaMoi[ten];
  if (!moiSchema) {
    pha.push(`xoá schema     ${ten}`);
    continue;
  }
  for (const truong of Object.keys(cuSchema.properties ?? {})) {
    if (!moiSchema.properties?.[truong]) {
      pha.push(`xoá trường     ${ten}.${truong}`);
    }
  }
  const batBuocCu = new Set(cuSchema.required ?? []);
  for (const truong of moiSchema.required ?? []) {
    if (!batBuocCu.has(truong)) {
      pha.push(`thêm trường bắt buộc  ${ten}.${truong}`);
    }
  }
}

for (const ten of Object.keys(schemaMoi)) {
  if (!schemaCu[ten]) them.push(`thêm schema    ${ten}`);
}

function soSanhThamSo(duongDan, phuongThuc, cuThaoTac, moiThaoTac) {
  const ten = (p) => `${p.name ?? p.$ref}`;
  const cuThamSo = new Map((cuThaoTac.parameters ?? []).map((p) => [ten(p), p]));
  const moiThamSo = new Map((moiThaoTac.parameters ?? []).map((p) => [ten(p), p]));

  for (const [k] of cuThamSo) {
    if (!moiThamSo.has(k)) {
      pha.push(`xoá tham số    ${phuongThuc.toUpperCase()} ${duongDan} · ${k}`);
    }
  }
  for (const [k, p] of moiThamSo) {
    if (!cuThamSo.has(k) && p.required === true) {
      pha.push(`thêm tham số bắt buộc  ${phuongThuc.toUpperCase()} ${duongDan} · ${k}`);
    }
  }
}

function docTuGit(dich) {
  const kq = spawnSync('git', ['show', dich], { cwd: GOC, encoding: 'utf8' });
  return kq.status === 0 ? kq.stdout : null;
}

console.log(`So với ${nhanhGoc}\n`);
for (const d of them) console.log(`  tương thích   ${d}`);
if (them.length) console.log('');

if (pha.length === 0) {
  console.log('Tương thích ngược: ĐẠT');
  process.exit(0);
}

console.error(`Thay đổi PHÁ VỠ tương thích (${pha.length}):\n`);
for (const d of pha) console.error(`  ${d}`);
console.error(`
Đổi phá vỡ tương thích thì lên phiên bản đường dẫn /api/v2/ — docs/13 mục 3.
Nếu đây là chủ ý và chưa có client nào dùng, ghi rõ lý do trong pull request.`);
process.exit(1);
