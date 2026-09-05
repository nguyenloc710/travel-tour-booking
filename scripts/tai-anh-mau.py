#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tải ảnh mẫu cho môi trường DEV về `web/apps/site/public/img/`.

    python scripts/tai-anh-mau.py

Thay cho `sinh-anh-mau.py` — bản đó vẽ hình khối phẳng bằng SVG, đủ để kiểm bố
cục nhưng nhìn ra ngay là đồ giả, và một website bán tour thì ảnh CHÍNH LÀ sản
phẩm. Bộ sinh cũ vẫn giữ lại: nó chạy được khi không có mạng.

--------------------------------------------------------------------- giấy phép

`docs/24` mục 8 có một dòng "Ảnh tìm trên mạng — Không dùng", và tệp này KHÔNG
vi phạm nó. Ý của quy tắc nằm ở câu đầu mục: **không dùng ảnh chưa rõ nguồn**.
Bảng ngay dưới đó cho phép "ảnh mua có giấy phép" với điều kiện lưu chứng từ và
ghi rõ phạm vi dùng — và đó đúng là thứ tệp này làm:

  nguồn      Unsplash
  giấy phép  Unsplash License — dùng được cả cho mục đích thương mại, không bắt
             buộc ghi công (https://unsplash.com/license)
  phạm vi    dữ liệu mồi cho môi trường dev
  hạn dùng   không có

Mục 8 còn đòi bốn thông tin đó phải lưu lại được, và ghi rằng hệ thống chưa có
chỗ lưu nên "giữ chúng trong một sổ ngoài hệ thống". Sổ đó là `NGUON.md` mà tệp
này sinh ra cạnh bộ ảnh, một dòng cho mỗi tấm, kèm liên kết về trang gốc.

**Ảnh TẢI VỀ, không nhúng thẳng từ CDN.** Nhúng thẳng thì mỗi lượt xem trang gửi
địa chỉ IP của khách sang một CDN Mỹ mà khách không hề đồng ý — đúng dạng việc
Google Fonts bị phạt ở châu Âu, và khách của dự án này ở Đan Mạch (`docs/31`).
Tải về còn bỏ được một phụ thuộc lúc chạy: Unsplash sập thì trang vẫn có ảnh.

Đây vẫn là ảnh **mẫu**, không phải ảnh của sản phẩm: không tấm nào chụp đúng
khách sạn hay đúng chuyến đi mà nó đang minh hoạ. Có ảnh thật thì thay hết.
"""

import io
import os
import sys
import urllib.request

GOC = os.path.join('web', 'apps', 'site', 'public', 'img')
CDN = 'https://images.unsplash.com/'

# Bề rộng theo chỗ dùng, không phải một cỡ cho tất cả: ảnh khối mở đầu trải hết
# khung 72rem, ảnh thẻ chỉ rộng chừng một phần ba khung.
RONG = {'hero': 2000, 'tour': 1400, 'khach-san': 900, 'blog': 1100}

# (đường dẫn, mã ảnh Unsplash, mã trang gốc, mô tả để đối chiếu khi thay ảnh)
#
# Mô tả lấy từ chính chú thích của Unsplash, không phải tôi đặt: nó là thứ dùng
# để kiểm xem tấm ảnh có đúng chủ đề không mà không phải mở từng cái.
ANH = [
    # ---------------------------------------------------------- khối mở đầu
    ('hero.jpg', 'photo-1748102289186-f27325fbdc7b', 'AZNY-IUVPTQ',
     'rocky islands rise from turquoise water'),

    # ------------------------------------------------------ ảnh bìa sản phẩm
    ('tour/p01-bac-nam.jpg', 'photo-1748102288847-b2607862c249', '8-hEg1OuFn8',
     'aerial view of islands in a bay'),
    ('tour/p02-du-thuyen-ha-long.jpg', 'photo-1668000018482-a02acf02b22a', 'TvB_S0cB2ik',
     'a group of boats in the water with ha long bay in the background'),
    ('tour/p03-hoi-an.jpg', 'photo-1676019556644-25abbce12a58', 'BOKe6DGnpaU',
     'a group of people walking down a street under paper lanterns'),
    ('tour/p04-sa-pa.jpg', 'photo-1609412058473-c199497c3c5d', 'PeRt3uMmjYM',
     'rice terraces in mu cang chai'),
    ('tour/p05-mien-trung.jpg', 'photo-1618165220283-e85246c4171c', '-nosLdNjHY0',
     'brown and green pagoda temple under blue sky'),
    ('tour/p06-gia-dinh.jpg', 'photo-1686766219304-5e2fb0df9d2d', 'wl_3yExsNsg',
     'a river running through a lush green valley'),
    ('tour/p07-du-thuyen-mekong.jpg', 'photo-1589562337460-9d825d2cd5a2', 'z3cTN0mSNko',
     'people riding on boat on river during daytime'),
    ('tour/p08-hoi-an-phu-quoc.jpg', 'photo-1693282814784-649be45a459b', 'IVJVh1v1PVs',
     'a beach with palm trees and a boat in the water'),
    ('tour/p09-am-thuc-ha-noi.jpg', 'photo-1583316175701-0bc5f25a0a44', 'dq03aws4SmY',
     'white noodles with meat and vegetables on white ceramic bowl'),
    ('tour/p10-phu-quoc.jpg', 'photo-1578458329607-534298aebc4d', 'jWECaopdAH8',
     'coconut trees on the beach'),
    ('tour/p11-mien-nam.jpg', 'photo-1543411789-1a67a2ac05c6', 'N4nqa6bc7n0',
     'group of people riding on canoe'),
    ('tour/p12-ninh-binh.jpg', 'photo-1557750255-c76072a7aad1', 'PgSxHidgJHQ',
     'pagoda surrounded by body of water and mountains'),

    # ---------------------------------------------------------- khách sạn
    ('khach-san/metropole.jpg', 'photo-1600869080148-338f85fb17f8', 'FSVe66WJAMo',
     'white and brown concrete building'),
    ('khach-san/paradise-ha-long.jpg', 'photo-1680357981460-f00398bafd42', 'HKqLxPfrkH8',
     'a large boat floating on top of a large body of water'),
    ('khach-san/azerai-hue.jpg', 'photo-1630809718582-2bc0a1b7b296', 'McTs3-iLuMY',
     'green palm trees near swimming pool during daytime'),
    ('khach-san/almanity-hoi-an.jpg', 'photo-1701249873912-0340c7f86c16', 'KwVYkAmbxx4',
     'a large swimming pool surrounded by palm trees'),
    ('khach-san/topas-sa-pa.jpg', 'photo-1696276959983-2da1448e695a', 'GE381hCl2L0',
     'a village nestled in a valley surrounded by mountains'),
    ('khach-san/victoria-can-tho.jpg', 'photo-1589553707339-0156415347ca', '1kiP6MaV1ao',
     'brown wooden dock on body of water during daytime'),
    ('khach-san/salinda-phu-quoc.jpg', 'photo-1565503187147-6b0012bab4da', 'Rp4HUchyUQY',
     'a row of umbrellas sitting on top of a sandy beach'),
    ('khach-san/tam-coc.jpg', 'photo-1545172538-171a802bd867', 'Mt1iIhkBk1I',
     'group of people on boat paddling during daytime'),
    ('khach-san/fusion-da-nang.jpg', 'photo-1641175994857-1d9bd5c6f678', 'LuKSjp7zzbw',
     'a large swimming pool surrounded by palm trees'),
    ('khach-san/des-arts-sai-gon.jpg', 'photo-1583417319070-4a69db38a482', 'wUk2U5Wirxg',
     'city skyline during night time'),

    # -------------------------------------------------------------- bài viết
    ('blog/pho.jpg', 'photo-1579856896394-07dfa10d7c5b', 'U1Ixm2MJYUk',
     'soup dish on white ceramic bowl'),
    ('blog/hoi-an.jpg', 'photo-1563354860-799d15199ac3', 'sVQyMuhy9Ug',
     'assorted color chinese lanterns at night'),
    ('blog/visum.jpg', 'photo-1503432697506-6986abec65ca', 'Jqk3VXErDF0',
     'people standing in the street near building'),
    ('blog/mua-mua.jpg', 'photo-1570366583862-f91883984fde', '9r2yeRccyls',
     'terraced rice fields in mountain valley'),
    ('blog/tau-hoa.jpg', 'photo-1726346234848-a6c0e78efd8c', 'EKFPwbPkDec',
     'a train traveling down train tracks next to a crowd of people'),
    ('blog/cho-noi.jpg', 'photo-1673675865894-00df0e4fe6cd', '0Wb7O7uTPWs',
     'a woman sitting in a boat filled with lots of items'),
]


def rong_cho(duong):
    thu_muc = duong.split('/')[0] if '/' in duong else 'hero'
    return RONG.get(thu_muc, 1200)


def tai(duong, ma):
    """Tải một tấm. Trả về số byte, hoặc None nếu hỏng.

    Kiểm content-type chứ không chỉ kiểm mã 200: Unsplash trả trang lỗi dạng
    HTML với mã 200 cho vài trường hợp, và ghi một trang HTML vào tệp .jpg là
    kiểu hỏng chỉ lộ ra khi mở trình duyệt.
    """
    url = '%s%s?w=%d&q=72&fm=jpg&fit=crop&crop=entropy' % (CDN, ma, rong_cho(duong))
    yc = urllib.request.Request(url, headers={'User-Agent': 'travel-tour-booking/dev'})
    try:
        with urllib.request.urlopen(yc, timeout=60) as tl:
            kieu = tl.headers.get('Content-Type', '')
            noi_dung = tl.read()
    except Exception as loi:                       # noqa: BLE001 — báo rồi bỏ qua
        print('  HỎNG  %-34s %s' % (duong, loi))
        return None

    if not kieu.startswith('image/'):
        print('  HỎNG  %-34s content-type %s' % (duong, kieu))
        return None
    if len(noi_dung) < 10000:
        print('  HỎNG  %-34s chỉ %d byte' % (duong, len(noi_dung)))
        return None

    dich = os.path.join(GOC, *duong.split('/'))
    thu_muc = os.path.dirname(dich)
    if not os.path.isdir(thu_muc):
        os.makedirs(thu_muc)
    with open(dich, 'wb') as f:
        f.write(noi_dung)
    return len(noi_dung)


def so_giay_phep(xong):
    """Sổ nguồn ảnh — bốn thông tin `docs/24` mục 8 đòi phải biết cho mỗi ảnh."""
    d = []
    d.append('# Nguồn ảnh mẫu')
    d.append('')
    d.append('> Tệp này do `scripts/tai-anh-mau.py` sinh ra. Đừng sửa tay.')
    d.append('')
    d.append('`docs/24` mục 8 đòi với mỗi ảnh phải biết **nguồn · giấy phép ·')
    d.append('phạm vi · hạn dùng**, và ghi rằng hệ thống chưa có chỗ lưu bốn thông tin')
    d.append('đó nên phải giữ trong một sổ ngoài hệ thống. Đây là sổ đó.')
    d.append('')
    d.append('| | |')
    d.append('|---|---|')
    d.append('| Nguồn | [Unsplash](https://unsplash.com) |')
    d.append('| Giấy phép | [Unsplash License](https://unsplash.com/license) — dùng được cho mục đích thương mại, không bắt buộc ghi công |')
    d.append('| Phạm vi | Dữ liệu mồi cho môi trường dev. **Không** dùng cho bản chạy thật |')
    d.append('| Hạn dùng | Không có |')
    d.append('')
    d.append('**Đây là ảnh mẫu, không phải ảnh của sản phẩm.** Không tấm nào chụp đúng')
    d.append('khách sạn hay đúng chuyến đi nó đang minh hoạ. Ảnh thật thay vào thì xoá')
    d.append('tệp này cùng với bộ ảnh.')
    d.append('')
    d.append('Cột cuối là chú thích gốc của Unsplash — dùng để đối chiếu chủ đề mà không')
    d.append('phải mở từng tấm.')
    d.append('')
    d.append('| Tệp | Trang gốc | Chú thích gốc |')
    d.append('|---|---|---|')
    for duong, _ma, trang, mo_ta in xong:
        d.append('| `%s` | https://unsplash.com/photos/%s | %s |' % (duong, trang, mo_ta))
    d.append('')
    io.open(os.path.join(GOC, 'NGUON.md'), 'w', encoding='utf-8').write('\n'.join(d))


def main():
    tong, xong, hong = 0, [], 0
    for duong, ma, trang, mo_ta in ANH:
        n = tai(duong, ma)
        if n is None:
            hong += 1
            continue
        tong += n
        xong.append((duong, ma, trang, mo_ta))
        print('  %-36s %6.0f KB' % (duong, n / 1024.0))

    so_giay_phep(xong)
    print('\n%d/%d tấm · %.1f MB · sổ nguồn ở %s/NGUON.md'
          % (len(xong), len(ANH), tong / 1048576.0, GOC))
    if hong:
        print('%d tấm HỎNG — sửa mã ảnh rồi chạy lại' % hong)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
