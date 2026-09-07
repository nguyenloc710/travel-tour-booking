#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Hook PostToolUse — kiểm file tài liệu ngay sau khi vừa sửa.

Chỉ chạy khi file vừa sửa nằm trong docs/ và là .md. Không chặn thao tác;
mã thoát 2 để đưa lỗi ngược lại cho Claude sửa ngay, thay vì để dồn tới CI.
"""
import json
import subprocess
import sys
from pathlib import Path

GOC = Path(__file__).resolve().parent.parent.parent


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

    if not (rel.startswith("docs/") and rel.endswith(".md")):
        return 0

    ket_qua = subprocess.run(
        [sys.executable, "scripts/docs_check.py", rel, "--im-lang"],
        cwd=GOC, capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if ket_qua.returncode == 0:
        return 0

    print(
        "Bộ kiểm tài liệu báo lỗi trên file vừa sửa. Sửa trước khi làm tiếp — "
        "quy ước ở docs/42-quy-trinh-tai-lieu.md mục 7.\n"
        + (ket_qua.stdout or "") + (ket_qua.stderr or ""),
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
