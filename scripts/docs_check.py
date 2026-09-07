#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Bộ kiểm tài liệu — cưỡng chế quy ước ở docs/42-quy-trinh-tai-lieu.md.

Chỉ dùng thư viện chuẩn. Không cài thêm gì (CLAUDE.md quy tắc 8).

    python scripts/docs_check.py                 kiểm toàn bộ
    python scripts/docs_check.py docs/02-...md   kiểm một file
    python scripts/docs_check.py --truy-vet      ma trận YC / QT / RB
    python scripts/docs_check.py --sua [file...] đặt "Cập nhật" thành hôm nay
    python scripts/docs_check.py --im-lang       chỉ in lỗi, bỏ cảnh báo

Mã thoát: 0 không lỗi · 1 có lỗi.
"""
from __future__ import annotations

import datetime as dt
import re
import subprocess
import sys
from pathlib import Path

GOC = Path(__file__).resolve().parent.parent

# Windows console mặc định không phải UTF-8 — chữ Việt và æ ø å sẽ vỡ nếu không đặt.
for _luong in (sys.stdout, sys.stderr):
    try:
        _luong.reconfigure(encoding="utf-8")
    except Exception:
        pass

# ---------------------------------------------------------------- hằng số

TRANG_THAI_HOP_LE = {"Nháp", "Đang duyệt", "Đã duyệt", "Lỗi thời"}
TRUONG_BAT_BUOC = ["Trạng thái", "Cập nhật", "Nguồn sự thật về", "Không nói về"]
# ADR có khối riêng, ngắn hơn — một quyết định không có "nguồn sự thật".
TRANG_THAI_ADR = {"Đề xuất", "Đã chốt", "Bị thay thế"}
TRUONG_BAT_BUOC_ADR = ["Trạng thái", "Ngày"]
TRUONG_KHI_DA_DUYET = ["Phiên bản", "Chủ sở hữu", "Người duyệt"]
HAN_RA_SOAT_NGAY = 90

# docs/03 mục 8. Khoá là biểu thức, giá trị là thứ phải dùng thay.
THUAT_NGU_CAM = {
    r"\bTourType\b": "ProductType",
    r"\bexchangeRate\b": "không tồn tại — giá nhập riêng từng market",
    r"\bexchange_rate\b": "không tồn tại — giá nhập riêng từng market",
    r"\bContinent\b": "Region",
    r"\bCountry\b": "Destination",
    r"châu lục": "miền",
    r"tỷ giá": "giá nhập riêng cho từng market",
}

# Dấu hiệu cho thấy dòng đang *nói về* thuật ngữ cấm chứ không dùng nó.
DAU_HIEU_PHU_DINH = (
    "không", "cấm", "sai", "đừng", "bỏ", "tên cũ", "thay bằng",
    "loại bỏ", "tránh", "nhầm", "cũ của",
)

# File cố tình liệt kê thuật ngữ cấm.
MIEN_TRU_THUAT_NGU = {
    "docs/03-tu-vung-nghiep-vu.md",
    "docs/42-quy-trinh-tai-lieu.md",
}

MUC_LOI, MUC_CANH_BAO, MUC_GHI_CHU = "LỖI", "CẢNH BÁO", "GHI CHÚ"

# Tài liệu đã đăng ký ở bản đồ 00 nhưng chưa viết — gom lại, in một lần.
CHUA_VIET: dict[str, list[str]] = {}


class Ket_qua:
    def __init__(self) -> None:
        self.muc: list[tuple[str, str, int, str]] = []

    def them(self, muc: str, file: str, dong: int, thong_diep: str) -> None:
        self.muc.append((muc, file, dong, thong_diep))

    def loi(self, file: str, dong: int, td: str) -> None:
        self.them(MUC_LOI, file, dong, td)

    def canh_bao(self, file: str, dong: int, td: str) -> None:
        self.them(MUC_CANH_BAO, file, dong, td)

    def ghi_chu(self, file: str, dong: int, td: str) -> None:
        self.them(MUC_GHI_CHU, file, dong, td)

    def dem(self, muc: str) -> int:
        return sum(1 for m, *_ in self.muc if m == muc)


# ---------------------------------------------------------------- tiện ích

def duong_dan_tuong_doi(p: Path) -> str:
    return p.relative_to(GOC).as_posix()


def doc(p: Path) -> str:
    return p.read_text(encoding="utf-8")


def file_tai_lieu() -> list[Path]:
    return sorted(GOC.glob("docs/**/*.md"))


def file_co_quy_uoc() -> list[Path]:
    """File chịu kiểm thuật ngữ và tham chiếu — gồm cả ba CLAUDE.md."""
    ds = file_tai_lieu()
    for ten in ("CLAUDE.md", "api/CLAUDE.md", "web/CLAUDE.md"):
        p = GOC / ten
        if p.exists():
            ds.append(p)
    return ds


def tach_khoi_trang_thai(noi_dung: str) -> tuple[dict[str, str], int]:
    """Trả về (các trường, số dòng khối bắt đầu). Khối rỗng nếu không tìm thấy."""
    dong = noi_dung.splitlines()
    for i, d in enumerate(dong[:12]):
        if d.strip() == "```":
            truong: dict[str, str] = {}
            for j in range(i + 1, min(i + 20, len(dong))):
                if dong[j].strip() == "```":
                    return truong, i + 1
                if ":" in dong[j]:
                    k, _, v = dong[j].partition(":")
                    k = k.strip()
                    if k and not k.startswith(" "):
                        truong[k] = v.strip()
            return truong, i + 1
    return {}, 0


def so_hieu_da_dang_ky() -> dict[str, str]:
    """Số hiệu tài liệu đã ghi trong bản đồ 00, kể cả file chưa viết."""
    ban_do = GOC / "docs/00-ke-hoach-tai-lieu.md"
    if not ban_do.exists():
        return {}
    return {m.group(1): m.group(0) for m in re.finditer(r"\b(\d{2})-[a-z0-9-]+", doc(ban_do))}


def file_theo_so_hieu() -> dict[str, Path]:
    return {p.name[:2]: p for p in GOC.glob("docs/*.md") if p.name[:2].isdigit()}


# ---------------------------------------------------------------- phép kiểm

def kiem_khoi_trang_thai(p: Path, kq: Ket_qua, hom_nay: dt.date) -> None:
    """Phép kiểm 1–4, 7."""
    rel = duong_dan_tuong_doi(p)
    truong, dong_khoi = tach_khoi_trang_thai(doc(p))
    la_adr = "/adr/" in rel

    if not truong:
        kq.loi(rel, 1, "thiếu khối trạng thái — xem 42 mục 2")
        return

    if la_adr:
        for t in TRUONG_BAT_BUOC_ADR:
            if t not in truong:
                kq.loi(rel, dong_khoi, f"ADR thiếu trường bắt buộc: {t}")
        tt = truong.get("Trạng thái", "").strip()
        if tt and not tt.startswith("Bị thay thế") and tt not in TRANG_THAI_ADR:
            kq.loi(rel, dong_khoi, f'trạng thái ADR không hợp lệ: "{tt}" — hợp lệ: {", ".join(sorted(TRANG_THAI_ADR))}')
        return

    for t in TRUONG_BAT_BUOC:
        if t not in truong:
            kq.loi(rel, dong_khoi, f"khối trạng thái thiếu trường bắt buộc: {t}")

    tt = truong.get("Trạng thái", "")
    tt_goc = tt.split("|")[0].strip() if "|" in tt else tt
    if tt_goc and not tt_goc.startswith("Bị thay thế bởi") and tt_goc not in TRANG_THAI_HOP_LE:
        kq.loi(rel, dong_khoi, f'trạng thái không hợp lệ: "{tt_goc}" — hợp lệ: {", ".join(sorted(TRANG_THAI_HOP_LE))}')

    ngay_txt = truong.get("Cập nhật", "")
    ngay = None
    if ngay_txt:
        try:
            ngay = dt.datetime.strptime(ngay_txt, "%d/%m/%Y").date()
        except ValueError:
            kq.loi(rel, dong_khoi, f'"Cập nhật" phải là dd/mm/yyyy, đang là "{ngay_txt}"')

    if tt_goc == "Đã duyệt":
        for t in TRUONG_KHI_DA_DUYET:
            if t not in truong:
                kq.loi(rel, dong_khoi, f'trạng thái "Đã duyệt" nhưng thiếu trường {t} — 42 mục 2')

    if ngay:
        qua_han = (hom_nay - ngay).days
        if qua_han > HAN_RA_SOAT_NGAY:
            kq.canh_bao(rel, dong_khoi, f"quá hạn rà soát {qua_han} ngày (ngưỡng {HAN_RA_SOAT_NGAY}) — 42 mục 8")
        elif ngay > hom_nay:
            kq.canh_bao(rel, dong_khoi, f'"Cập nhật" nằm ở tương lai: {ngay_txt}')


def kiem_tham_chieu(p: Path, kq: Ket_qua, dang_ky: dict[str, str], hien_co: dict[str, Path]) -> None:
    """Phép kiểm 5 và 8 — tham chiếu tài liệu và ADR."""
    rel = duong_dan_tuong_doi(p)
    so_minh = p.name[:2] if p.name[:2].isdigit() else None

    for i, dong in enumerate(doc(p).splitlines(), 1):
        # docs/NN-ten.md hoặc `NN-ten.md`
        for m in re.finditer(r"(?:docs/)?(\d{2})-([a-z0-9-]+)\.md", dong):
            so = m.group(1)
            if so in hien_co:
                that = hien_co[so].name
                if that != f"{so}-{m.group(2)}.md":
                    kq.loi(rel, i, f"tham chiếu {m.group(0)} nhưng file thật là {that}")
            elif so in dang_ky:
                CHUA_VIET.setdefault(so, []).append(f"{rel}:{i}")
            else:
                kq.loi(rel, i, f"trỏ tới {m.group(0)} — không tồn tại và không có trong bản đồ 00")

        # `NN` — quy ước trỏ tài liệu. Số không nằm trong bản đồ thì không phải
        # tham chiếu (dải số, số lượng file, số phiên bản…), bỏ qua im lặng.
        for m in re.finditer(r"`(\d{2})`", dong):
            so = m.group(1)
            if so == so_minh or so in hien_co:
                continue
            if so in dang_ky:
                CHUA_VIET.setdefault(so, []).append(f"{rel}:{i}")

        # ADR-00N
        for m in re.finditer(r"ADR-(\d{3})", dong):
            so = m.group(1)
            if not list(GOC.glob(f"docs/adr/{so}-*.md")):
                kq.canh_bao(rel, i, f"ADR-{so} chưa được viết")


def kiem_thuat_ngu(p: Path, kq: Ket_qua) -> None:
    """Phép kiểm 6 — thuật ngữ bị cấm ở 03 mục 8."""
    rel = duong_dan_tuong_doi(p)
    if rel in MIEN_TRU_THUAT_NGU:
        return

    dong = doc(p).splitlines()
    for i, d in enumerate(dong):
        cua_so = " ".join(dong[max(0, i - 2): i + 3]).lower()
        if any(dh in cua_so for dh in DAU_HIEU_PHU_DINH):
            continue
        for mau, thay_bang in THUAT_NGU_CAM.items():
            if re.search(mau, d):
                tu = re.search(mau, d).group(0)
                kq.loi(rel, i + 1, f'thuật ngữ bị cấm "{tu}" — dùng: {thay_bang} (03 mục 8)')


def kiem_nguon_su_that_trung(kq: Ket_qua) -> None:
    """42 mục 6 — hai file không được cùng chịu trách nhiệm một sự thật."""
    thay: dict[str, str] = {}
    for p in file_tai_lieu():
        truong, dong = tach_khoi_trang_thai(doc(p))
        nguon = truong.get("Nguồn sự thật về", "").strip().lower().rstrip(".")
        if not nguon:
            continue
        khoa = re.sub(r"[^a-zà-ỹ0-9 ]", " ", nguon)
        khoa = " ".join(sorted(set(khoa.split())))
        if khoa in thay:
            kq.canh_bao(duong_dan_tuong_doi(p), dong,
                        f'"Nguồn sự thật về" trùng với {thay[khoa]} — 42 mục 6')
        else:
            thay[khoa] = duong_dan_tuong_doi(p)


def kiem_ban_do(kq: Ket_qua, dang_ky: dict[str, str], hien_co: dict[str, Path]) -> None:
    """Phép kiểm 8 — file có thật mà chưa đăng ký vào bản đồ 00."""
    for so, p in sorted(hien_co.items()):
        if so not in dang_ky:
            kq.loi(duong_dan_tuong_doi(p), 1, "chưa đăng ký vào bản đồ docs/00 — 42 mục 9")


# ---------------------------------------------------------------- truy vết

MAU_MA = re.compile(r"\b(YC|QT|RB)-(\d{3})\b")


def truy_vet() -> int:
    dinh_nghia: dict[str, list[str]] = {}
    for p in file_co_quy_uoc():
        rel = duong_dan_tuong_doi(p)
        trong_hang_rao = False
        for i, dong in enumerate(doc(p).splitlines(), 1):
            if dong.lstrip().startswith("```"):
                trong_hang_rao = not trong_hang_rao
                continue
            if trong_hang_rao:  # ví dụ minh hoạ, không phải định nghĩa
                continue
            for m in MAU_MA.finditer(dong):
                dinh_nghia.setdefault(m.group(0), []).append(f"{rel}:{i}")

    thu_muc_test = [GOC / "api", GOC / "web"]
    trong_test: set[str] = set()
    for goc in thu_muc_test:
        if not goc.exists():
            continue
        for p in goc.rglob("*"):
            if p.is_file() and p.suffix in {".java", ".kt", ".ts", ".tsx"} and "test" in p.as_posix().lower():
                try:
                    for m in MAU_MA.finditer(p.read_text(encoding="utf-8", errors="ignore")):
                        trong_test.add(m.group(0))
                except OSError:
                    pass

    if not dinh_nghia:
        print("Chưa có mã YC / QT / RB nào trong tài liệu.")
        print("Cấp mã theo 40 mục 6 khi bắt đầu viết tài liệu tầng 2.")
        return 0

    print(f"{'Mã':<10} {'Định nghĩa ở':<46} Test")
    print("-" * 74)
    thieu = 0
    for ma in sorted(dinh_nghia):
        noi = dinh_nghia[ma][0]
        if len(dinh_nghia[ma]) > 1:
            noi += f" (+{len(dinh_nghia[ma]) - 1})"
        co = "có" if ma in trong_test else "THIẾU"
        thieu += 0 if ma in trong_test else 1
        print(f"{ma:<10} {noi:<46} {co}")
    print("-" * 74)
    print(f"{len(dinh_nghia)} mã · {thieu} chưa có test")
    if thieu:
        print("Lỗ hổng truy vết là cảnh báo cho tới cổng G4, sau đó là lỗi — 40 mục 6.")
    return 0


# ---------------------------------------------------------------- sửa ngày

def sua_ngay(duong_dan: list[str], hom_nay: dt.date) -> int:
    if not duong_dan:
        try:
            ra = subprocess.run(["git", "status", "--porcelain"], cwd=GOC,
                                capture_output=True, text=True, check=True).stdout
        except Exception as e:
            print(f"Không chạy được git: {e}")
            return 1
        duong_dan = [d[3:].strip() for d in ra.splitlines()
                     if d[3:].strip().startswith("docs/") and d.strip().endswith(".md")]

    if not duong_dan:
        print("Không có file tài liệu nào đang sửa dở.")
        return 0

    moi = hom_nay.strftime("%d/%m/%Y")
    for d in duong_dan:
        p = GOC / d
        if not p.exists():
            print(f"bỏ qua {d} — không tồn tại")
            continue
        noi_dung = doc(p)
        moi_nd, so = re.subn(r"(?m)^Cập nhật:.*$", f"Cập nhật: {moi}", noi_dung, count=1)
        if so and moi_nd != noi_dung:
            p.write_text(moi_nd, encoding="utf-8")
            print(f"đã đặt Cập nhật = {moi}  ·  {d}")
        else:
            print(f"không đổi  ·  {d}")
    return 0


# ---------------------------------------------------------------- điều phối

def chay(muc_tieu: list[Path], hom_nay: dt.date, im_lang: bool) -> int:
    kq = Ket_qua()
    dang_ky = so_hieu_da_dang_ky()
    hien_co = file_theo_so_hieu()
    toan_bo = len(muc_tieu) > 1

    for p in muc_tieu:
        if p.suffix != ".md":
            continue
        if p.as_posix().startswith((GOC / "docs").as_posix()):
            kiem_khoi_trang_thai(p, kq, hom_nay)
        kiem_tham_chieu(p, kq, dang_ky, hien_co)
        kiem_thuat_ngu(p, kq)

    if toan_bo:
        kiem_nguon_su_that_trung(kq)
        kiem_ban_do(kq, dang_ky, hien_co)

    for muc in (MUC_LOI, MUC_CANH_BAO, MUC_GHI_CHU):
        if im_lang and muc != MUC_LOI:
            continue
        nhom = [m for m in kq.muc if m[0] == muc]
        if not nhom:
            continue
        print(f"\n{muc}  ({len(nhom)})")
        for _, file, dong, td in nhom:
            print(f"  {file}:{dong}  {td}")

    if CHUA_VIET and not im_lang:
        print()
        print(f"CHƯA VIẾT  ({len(CHUA_VIET)} tài liệu đã đăng ký ở bản đồ 00, đang được trỏ tới)")
        for so in sorted(CHUA_VIET):
            print(f"  {so}  ·  {len(CHUA_VIET[so])} chỗ trỏ tới  ·  ví dụ {CHUA_VIET[so][0]}")

    so_loi = kq.dem(MUC_LOI)
    print(f"\n{len(muc_tieu)} file · {so_loi} lỗi · {kq.dem(MUC_CANH_BAO)} cảnh báo · {kq.dem(MUC_GHI_CHU)} ghi chú · {len(CHUA_VIET)} tài liệu chưa viết")
    if so_loi == 0:
        print("Bộ kiểm tài liệu: ĐẠT")
    return 1 if so_loi else 0


def main(argv: list[str]) -> int:
    hom_nay = dt.date.today()
    im_lang = "--im-lang" in argv
    argv = [a for a in argv if a != "--im-lang"]

    if "--truy-vet" in argv:
        return truy_vet()
    if "--sua" in argv:
        return sua_ngay([a for a in argv if a != "--sua"], hom_nay)

    duong_dan = [a for a in argv if not a.startswith("-")]
    if duong_dan:
        muc_tieu = [GOC / d if not Path(d).is_absolute() else Path(d) for d in duong_dan]
        thieu = [p for p in muc_tieu if not p.exists()]
        if thieu:
            for p in thieu:
                print(f"Không tìm thấy: {p}")
            return 1
    else:
        muc_tieu = file_co_quy_uoc()

    return chay(muc_tieu, hom_nay, im_lang)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
