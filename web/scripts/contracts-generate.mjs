#!/usr/bin/env node
/**
 * Sinh code từ `contracts/openapi.yaml` cho **CẢ HAI** phía.
 *
 * Một lệnh chứ không hai, vì đây là chỗ hai bên gặp nhau và sửa spec mà chỉ
 * sinh lại một phía là bắt đầu quá trình lệch âm thầm — thứ mà spec-first
 * (ADR-002) sinh ra để ngăn.
 *
 *     pnpm contracts:generate
 *
 * Phía Java cần JDK 17+ để chạy Gradle. Không có JDK thì script báo rõ và dừng,
 * không im lặng sinh mỗi phía TypeScript.
 */
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const WEB = join(dirname(fileURLToPath(import.meta.url)), '..');
const GOC = join(WEB, '..');
const laWindows = process.platform === 'win32';

/**
 * Chạy một lệnh dưới shell.
 *
 * `lenh` là **một chuỗi đầy đủ**, không phải cặp (lệnh, mảng tham số). Lý do:
 * trên Windows, `spawnSync('gradlew.bat', [...], { cwd, shell: true })` báo
 * *"'gradlew.bat' is not recognized"* — cmd.exe phân giải tên chương trình
 * theo thư mục hiện hành của **tiến trình cha**, không theo `cwd` được truyền
 * vào. Truyền đường dẫn tuyệt đối trong chuỗi thì hết mơ hồ. Dạng chuỗi cũng
 * tránh cảnh báo DEP0190 của Node khi vừa đưa mảng tham số vừa bật shell.
 */
function chay(mo_ta, lenh, cwd) {
  console.log(`\n▶ ${mo_ta}`);
  const kq = spawnSync(lenh, { cwd, stdio: 'inherit', shell: true });
  if (kq.status !== 0) {
    console.error(`\n✗ ${mo_ta} thất bại (mã ${kq.status}).`);
    process.exit(kq.status ?? 1);
  }
}

const spec = join(GOC, 'contracts', 'openapi.yaml');
if (!existsSync(spec)) {
  console.error(`Không tìm thấy ${spec}`);
  process.exit(1);
}

chay(
  'TypeScript client → web/packages/api-client/src',
  'pnpm --filter @travel/api-client run generate',
  WEB,
);

const gradlew = join(GOC, 'api', laWindows ? 'gradlew.bat' : 'gradlew');
// Task nằm ở dự án gốc `travel-api`, không còn tiền tố `:web:`: ADR-010 gộp bốn
// module Gradle thành một, và đường dẫn task cũ chết theo — `gradlew` báo
// "project 'web' not found" chứ không báo gì về nguyên nhân thật.
chay(
  'Interface Java → api/build/generated/openapi',
  `"${gradlew}" contractsGenerate`,
  join(GOC, 'api'),
);

console.log('\nĐã sinh xong cả hai phía.');
console.log('Bước tiếp theo: sửa controller cho khớp interface mới, rồi sửa frontend.');
