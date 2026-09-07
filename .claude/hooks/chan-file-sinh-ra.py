#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Hook PreToolUse — chặn sửa tay code sinh ra từ contracts/openapi.yaml.

CLAUDE.md quy tắc 9 và ADR-002: sửa spec, đừng sửa code sinh ra. Sửa tay thì
lần `contracts:generate` kế tiếp sẽ xoá sạch, và không ai biết vì sao hỏng.

Mã thoát 2 = chặn thao tác, lý do đi qua stderr về cho Claude.
"""
import json
import sys
from pathlib import Path

GOC = Path(__file__).resolve().parent.parent.parent

# Tiền tố đường dẫn bị chặn, kèm lý do và việc phải làm thay.
CHAN = [
    ("web/packages/api-client/",
     "TS client sinh từ contracts/openapi.yaml"),
    ("api/web/build/generated/",
     "interface Java sinh từ contracts/openapi.yaml"),
    ("api/build/generated/",
     "mã sinh lúc build"),
]

HUONG_DAN = (
    "Quy trình đúng (ADR-002, CLAUDE.md mục \"Đổi hợp đồng API\"):\n"
    "  1. Sửa contracts/openapi.yaml   ← pull request riêng, có người duyệt\n"
    "  2. pnpm contracts:generate\n"
    "  3. Sửa controller cho khớp interface mới\n"
    "  4. Sửa frontend"
)


def main() -> int:
    try:
        du_lieu = json.load(sys.stdin)
    except Exception:
        return 0

    duong_dan = (du_lieu.get("tool_input") or {}).get("file_path", "")
    if not duong_dan:
        return 0

    try:
        rel = Path(duong_dan).resolve().relative_to(GOC).as_posix()
    except ValueError:
        return 0

    for tien_to, mo_ta in CHAN:
        if rel.startswith(tien_to):
            print(
                f"CHẶN: {rel} là code sinh ra ({mo_ta}), không sửa tay.\n\n{HUONG_DAN}",
                file=sys.stderr,
            )
            return 2

    return 0


if __name__ == "__main__":
    sys.exit(main())
