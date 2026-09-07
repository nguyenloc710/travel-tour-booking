#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Đẩy bộ ảnh mẫu lên kho MinIO của môi trường dev.

    python scripts/nap-anh-len-kho.py

Chạy sau `scripts/tai-anh-mau.py` — tệp đó tải ảnh về đĩa, tệp này đưa chúng vào
kho. Chạy lại được: `mc mirror` chỉ đẩy tấm nào khác.

--------------------------------------------------------------- vì sao cần bước này

`docs/24` mục 7.1b nói bộ ảnh mẫu nằm ở `web/apps/site/public/img/`, và cho tới
nay `product.hero_image` trỏ thẳng vào đó — Next phục vụ tệp tĩnh, không ai phải
nghĩ tới kho.

Bộ ảnh của trang chi tiết thì không đi đường đó được. Nó là dữ liệu **biên tập
viên tự thêm** qua trang quản trị, nên nó phải nằm ở chỗ mà một lần tải lên lúc
chạy thật ghi vào được — tức là kho đối tượng (ADR-011). Hai nguồn ảnh sống song
song trong giai đoạn này là có chủ ý: ảnh hero còn ở `public/`, ảnh bộ đã ở kho.
Gom về một chỗ là việc riêng, không làm lẫn vào đây.

-------------------------------------------------------------------- không cần SDK

Dùng `mc` chạy trong Docker chứ không cài thư viện S3 cho Python: đường ĐỌC của
API không cần client S3 nào cả — nó chỉ ghép `media_asset.path` với địa chỉ gốc
trong cấu hình. Client S3 chỉ cần cho đường GHI ở trang quản trị, và đó là việc
sau.

Địa chỉ dùng `host.docker.internal` thay vì tên mạng của compose: tên mạng phụ
thuộc tên thư mục dự án, còn cổng 9000 thì compose đã mở ra máy thật rồi.
"""

import os
import subprocess
import sys

GOC = os.path.join('web', 'apps', 'site', 'public', 'img')
BUCKET = 'travel-media'
MC = 'minio/mc:RELEASE.2025-04-16T18-13-26Z'
KHO = 'http://host.docker.internal:9000'
NGUOI_DUNG = os.environ.get('STORAGE_USER', 'travel')
MAT_KHAU = os.environ.get('STORAGE_PASSWORD', 'travel-dev-secret')


def main():
    if not os.path.isdir(GOC):
        print('Không thấy %s — chạy scripts/tai-anh-mau.py trước.' % GOC)
        return 1

    tuyet_doi = os.path.abspath(GOC).replace(os.sep, '/')
    lenh = (
        'mc alias set kho %s %s %s >/dev/null && '
        'mc mirror --overwrite /anh kho/%s' % (KHO, NGUOI_DUNG, MAT_KHAU, BUCKET)
    )

    # MSYS_NO_PATHCONV: Git Bash trên Windows biến "/anh" thành một đường dẫn
    # Windows trước khi Docker nhìn thấy nó.
    moi_truong = dict(os.environ, MSYS_NO_PATHCONV='1')

    ket_qua = subprocess.run(
        ['docker', 'run', '--rm',
         '-v', '%s:/anh:ro' % tuyet_doi,
         '--entrypoint', 'sh', MC, '-c', lenh],
        env=moi_truong)

    if ket_qua.returncode != 0:
        print('\nHỎNG. Kho đã chạy chưa? `docker compose up -d`')
        return ket_qua.returncode

    print('\nXong. Xem bằng mắt ở http://localhost:9001 (travel / travel-dev-secret).')
    print('Đường dẫn trong CSDL là TƯƠNG ĐỐI — `tour/p01-bac-nam.jpg`, không phải URL đầy đủ.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
