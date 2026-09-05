#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Sinh bộ ảnh mẫu cho môi trường DEV.

    python scripts/sinh-anh-mau.py

Dữ liệu mồi trỏ tới `/img/…` từ ngày đầu, nhưng chưa bao giờ có tệp thật nào ở
đó — nên mọi thẻ sản phẩm, mọi ảnh bìa bài viết đều hiện khung vỡ, và không ai
nhìn ra được bố cục thật sự trông thế nào. Ảnh vỡ còn che mất lỗi bố cục: một
thẻ có ảnh cao 320px xếp khác hẳn một thẻ có ảnh cao 0px.

**Đây là ảnh GIẢ, không phải ảnh của sản phẩm.** Chúng là hình khối phẳng —
trời, núi, nước, mặt trời — đủ để mắt đọc ra "đây là một tấm ảnh phong cảnh" và
để kiểm bố cục, tương phản chữ trên ảnh, tỷ lệ khung. Ảnh thật do biên tập viên
tải lên qua M11 (`docs/22`), và khi có ảnh thật thì xoá cả thư mục này đi.

Vì sao SVG chứ không JPEG: không phụ thuộc thư viện nào (`CLAUDE.md` điều 8 —
không tự ý cài thêm dependency), tệp vài KB, và `next/image` với `unoptimized`
phục vụ thẳng được. Đổi lại chúng không nén ảnh thật — điều đó không quan trọng
với dữ liệu dev.
"""

import io
import os

GOC = os.path.join('web', 'apps', 'site', 'public', 'img')

# Mỗi bảng màu là một địa điểm. Thứ tự: trời trên, trời dưới, dãy núi xa, dãy
# núi gần, mặt nước, điểm nhấn (mặt trời hoặc đèn).
BANG_MAU = {
    'halong':   ('#1b3a5c', '#4a7fa5', '#2c5470', '#16303f', '#3f6f8f', '#f2b544'),
    'hanoi':    ('#3d2c4a', '#8a5a6d', '#4a3550', '#2a1c33', '#6d4a5c', '#e8a13a'),
    'hoian':    ('#2a1f3d', '#b5563f', '#402a45', '#241a2e', '#7a4038', '#ffc861'),
    'mekong':   ('#1f3d2c', '#7aa05a', '#2f5038', '#1a2e22', '#4f7a55', '#f0d264'),
    'sapa':     ('#2c3e50', '#7f9bb0', '#3e5c6b', '#24343d', '#5c7a86', '#e6e0cf'),
    'hue':      ('#33283a', '#9c7a5c', '#4a3a42', '#282029', '#6b5548', '#dfb072'),
    'phuquoc':  ('#0f3a4a', '#4fb3c4', '#1c5a63', '#0d3038', '#2f8f9e', '#ffd97a'),
    'ninhbinh': ('#243b2e', '#6d9b6a', '#33553c', '#1c2e24', '#4a7a58', '#f2c85b'),
    'saigon':   ('#2b2233', '#a4636b', '#3b2c40', '#221a28', '#6b4750', '#f0a24a'),
    'danang':   ('#153044', '#5f93b5', '#255066', '#12262f', '#3a7690', '#ffcf6b'),
}

# Kiểu bố cục — cùng một bảng màu nhưng khác hình khối, để hai tấm cạnh nhau
# trong lưới không trông như một tấm lặp lại.
KIEU = ('nui', 'vinh', 'ruong', 'pho')


def _duong_nui(w, h, dinh, lech, chan):
    """Đường gấp khúc làm silhouette núi. `dinh` là danh sách (x tỷ lệ, y tỷ lệ)."""
    diem = ['%g,%g' % (0, chan * h)]
    for tx, ty in dinh:
        diem.append('%g,%g' % ((tx + lech) * w, ty * h))
    diem.append('%g,%g' % (w, chan * h))
    diem.append('%g,%g' % (w, h))
    diem.append('%g,%g' % (0, h))
    return ' '.join(diem)


def sinh(ten, mau, kieu, w=1200, h=800):
    troi_tren, troi_duoi, nui_xa, nui_gan, nuoc, nhan = BANG_MAU[mau]
    ma = '%s-%s' % (mau, kieu)

    r = []
    a = r.append
    a('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" '
      'width="%d" height="%d" role="img">' % (w, h, w, h))
    a('<defs>')
    a('<linearGradient id="troi-%s" x1="0" y1="0" x2="0" y2="1">' % ma)
    a('<stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/>' % (troi_tren, troi_duoi))
    a('</linearGradient>')
    a('<linearGradient id="nuoc-%s" x1="0" y1="0" x2="0" y2="1">' % ma)
    a('<stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/>' % (nuoc, troi_tren))
    a('</linearGradient>')
    a('</defs>')

    a('<rect width="%d" height="%d" fill="url(#troi-%s)"/>' % (w, h, ma))

    # Mặt trời / mặt trăng. Đặt lệch tâm theo quy tắc một phần ba.
    cx = w * (0.72 if kieu in ('nui', 'ruong') else 0.28)
    a('<circle cx="%g" cy="%g" r="%g" fill="%s" opacity="0.9"/>'
      % (cx, h * 0.30, w * 0.055, nhan))

    if kieu == 'nui':
        a('<polygon points="%s" fill="%s" opacity="0.75"/>'
          % (_duong_nui(w, h, [(0.10, 0.44), (0.26, 0.30), (0.44, 0.47), (0.62, 0.34),
                               (0.82, 0.50), (1.00, 0.40)], 0, 0.52), nui_xa))
        a('<polygon points="%s" fill="%s"/>'
          % (_duong_nui(w, h, [(0.14, 0.60), (0.34, 0.48), (0.55, 0.62), (0.78, 0.52),
                               (1.00, 0.64)], 0, 0.68), nui_gan))
        a('<rect y="%g" width="%d" height="%g" fill="url(#nuoc-%s)"/>'
          % (h * 0.74, w, h * 0.26, ma))
    elif kieu == 'vinh':
        # Đá vôi dựng đứng trên mặt nước — hình khối đặc trưng của vịnh.
        for i, (x, cao, rong) in enumerate(
                [(0.08, 0.30, 0.09), (0.22, 0.42, 0.07), (0.40, 0.26, 0.11),
                 (0.58, 0.38, 0.08), (0.76, 0.24, 0.10), (0.90, 0.34, 0.07)]):
            mau_da = nui_xa if i % 2 else nui_gan
            day = h * 0.68
            a('<polygon points="%g,%g %g,%g %g,%g %g,%g" fill="%s" opacity="0.92"/>'
              % (x * w, day,
                 (x + rong * 0.30) * w, day - cao * h,
                 (x + rong * 0.72) * w, day - cao * h * 0.86,
                 (x + rong) * w, day,
                 mau_da))
        a('<rect y="%g" width="%d" height="%g" fill="url(#nuoc-%s)"/>'
          % (h * 0.68, w, h * 0.32, ma))
        # Bóng nước: lặp lại khối đá, lật ngược, mờ đi.
        a('<g opacity="0.22" transform="translate(0,%g) scale(1,-0.45)">' % (h * 1.36))
        for i, (x, cao, rong) in enumerate(
                [(0.08, 0.30, 0.09), (0.40, 0.26, 0.11), (0.76, 0.24, 0.10)]):
            day = h * 0.68
            a('<polygon points="%g,%g %g,%g %g,%g" fill="%s"/>'
              % (x * w, day, (x + rong * 0.5) * w, day - cao * h, (x + rong) * w, day, nui_gan))
        a('</g>')
    elif kieu == 'ruong':
        a('<polygon points="%s" fill="%s" opacity="0.7"/>'
          % (_duong_nui(w, h, [(0.20, 0.34), (0.50, 0.24), (0.80, 0.36)], 0, 0.46), nui_xa))
        # Ruộng bậc thang: những dải cong chồng lên nhau, mỗi dải nhạt dần.
        for i in range(7):
            y = h * (0.50 + i * 0.072)
            do_cong = h * 0.05
            a('<path d="M0 %g Q %g %g %d %g L %d %d L 0 %d Z" fill="%s" opacity="%.2f"/>'
              % (y, w * 0.5, y + do_cong, w, y - do_cong * 0.4, w, h, h,
                 nui_gan if i % 2 else nuoc, 0.35 + i * 0.09))
    else:  # 'pho'
        # Phố: khối nhà cao thấp, cửa sổ sáng, đèn lồng treo.
        chan = h * 0.78
        rong = w / 14.0
        cao = [0.30, 0.44, 0.22, 0.38, 0.26, 0.48, 0.20, 0.34,
               0.42, 0.24, 0.36, 0.28, 0.46, 0.32]
        for i, c in enumerate(cao):
            x = i * rong
            a('<rect x="%g" y="%g" width="%g" height="%g" fill="%s" opacity="%.2f"/>'
              % (x, chan - c * h, rong - 3, c * h + h * 0.22,
                 nui_gan if i % 2 else nui_xa, 0.88))
            for j in range(int(c * 10)):
                if (i + j) % 3:
                    continue
                a('<rect x="%g" y="%g" width="10" height="14" fill="%s" opacity="0.65"/>'
                  % (x + rong * 0.30, chan - c * h + 20 + j * 26, nhan))
        for i in range(6):
            a('<circle cx="%g" cy="%g" r="9" fill="%s" opacity="0.8"/>'
              % (w * (0.08 + i * 0.17), h * (0.16 + (i % 3) * 0.05), nhan))

    # Lớp phủ tối ở đáy: chỗ chữ đè lên ảnh trong thẻ và ở hero. Không có nó thì
    # chữ trắng trên trời sáng không đọc được, và đó là lỗi chỉ lộ ra khi có ảnh.
    a('<linearGradient id="phu-%s" x1="0" y1="0" x2="0" y2="1">'
      '<stop offset="0" stop-color="#000" stop-opacity="0"/>'
      '<stop offset="1" stop-color="#000" stop-opacity="0.45"/></linearGradient>' % ma)
    a('<rect y="%g" width="%d" height="%g" fill="url(#phu-%s)"/>' % (h * 0.55, w, h * 0.45, ma))
    a('</svg>')

    duong = os.path.join(GOC, ten)
    thu_muc = os.path.dirname(duong)
    if not os.path.isdir(thu_muc):
        os.makedirs(thu_muc)
    io.open(duong, 'w', encoding='utf-8').write('\n'.join(r) + '\n')
    return duong


# Danh sách này phải khớp với các đường dẫn trong api/scripts/seed-dev.sql.
# Lệch một cái là một khung ảnh vỡ trên màn hình.
ANH = [
    # ----------------------------------------------------------- sản phẩm
    ('tour/bac-nam.svg',          'halong',   'vinh'),
    ('tour/halong.svg',           'halong',   'vinh'),
    ('tour/hoi-an.svg',           'hoian',    'pho'),
    ('tour/sapa.svg',             'sapa',     'ruong'),
    ('tour/mekong.svg',           'mekong',   'nui'),
    ('tour/hue-danang.svg',       'hue',      'nui'),
    ('tour/phu-quoc.svg',         'phuquoc',  'vinh'),
    ('tour/ninh-binh.svg',        'ninhbinh', 'vinh'),
    ('tour/saigon.svg',           'saigon',   'pho'),
    ('tour/ha-noi-pho.svg',       'hanoi',    'pho'),
    ('tour/rieng-gia-dinh.svg',   'ninhbinh', 'ruong'),
    ('tour/combo-da-nang.svg',    'danang',   'nui'),
    # -------------------------------------------------------------- bản đồ
    ('ban-do/bac-nam.svg',        'sapa',     'nui'),
    ('ban-do/mien-trung.svg',     'hue',      'ruong'),
    # ---------------------------------------------------------- khách sạn
    ('khach-san/metropole.svg',   'hanoi',    'pho'),
    ('khach-san/emeraude.svg',    'halong',   'vinh'),
    ('khach-san/anantara.svg',    'hue',      'nui'),
    ('khach-san/almanity.svg',    'hoian',    'pho'),
    ('khach-san/topas.svg',       'sapa',     'ruong'),
    ('khach-san/victoria.svg',    'mekong',   'nui'),
    ('khach-san/salinda.svg',     'phuquoc',  'vinh'),
    ('khach-san/tam-coc.svg',     'ninhbinh', 'ruong'),
    ('khach-san/fusion.svg',      'danang',   'vinh'),
    ('khach-san/des-arts.svg',    'saigon',   'pho'),
    # ------------------------------------------------------------- tham quan
    ('tham-quan/van-mieu.svg',    'hanoi',    'pho'),
    ('tham-quan/hang-sung-sot.svg','halong',  'vinh'),
    ('tham-quan/den-long.svg',    'hoian',    'pho'),
    ('tham-quan/cho-noi.svg',     'mekong',   'nui'),
    # ---------------------------------------------------------------- blog
    ('blog/pho.svg',              'hanoi',    'pho'),
    ('blog/hoi-an.svg',           'hoian',    'pho'),
    ('blog/visum.svg',            'saigon',   'pho'),
    ('blog/mua-mua.svg',          'mekong',   'ruong'),
    ('blog/tau-hoa.svg',          'danang',   'nui'),
    ('blog/cho-noi.svg',          'mekong',   'nui'),
    # -------------------------------------------------------- ảnh nền trang chủ
    ('hero.svg',                  'halong',   'vinh'),
]


def main():
    for ten, mau, kieu in ANH:
        sinh(ten, mau, kieu)
    print('%d ảnh vào %s' % (len(ANH), GOC))


if __name__ == '__main__':
    main()
