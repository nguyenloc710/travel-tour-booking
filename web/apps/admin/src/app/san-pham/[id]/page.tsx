'use client';

import Link from 'next/link';
import { use, useCallback, useEffect, useState } from 'react';
import type { AdminProductDetail, AdminProductTranslation } from '@travel/api-client';
import { adminApi, laChuaDangNhap, loiTiengViet } from '@/lib/api';
import { TabChung } from '@/components/TabChung';
import { TabDich } from '@/components/TabDich';
import { TabNgayKhoiHanh } from '@/components/TabNgayKhoiHanh';
import { TabThiTruong } from '@/components/TabThiTruong';

const TAB = [
  ['chung', 'Thông tin chung'],
  ['dich', 'Bản dịch'],
  ['thi-truong', 'Thị trường và bán'],
  ['ngay', 'Ngày khởi hành và giá'],
] as const;

type Tab = (typeof TAB)[number][0];

/**
 * Sửa một sản phẩm (docs/22 M3).
 *
 * Dải trạng thái ở đầu trang trả lời câu hỏi *"vì sao tour này chưa hiện trên
 * web"* bằng cách nhìn, không bằng cách hỏi lập trình viên — docs/22 mục 5.
 */
export default function SuaSanPham({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [sp, setSp] = useState<AdminProductDetail | null>(null);
  const [banDich, setBanDich] = useState<AdminProductTranslation[]>([]);
  const [loi, setLoi] = useState('');
  const [tab, setTab] = useState<Tab>('chung');

  const nap = useCallback(async () => {
    const api = adminApi();
    try {
      const [chiTiet, dich] = await Promise.all([
        api.chiTietSanPhamQuanTri({ id }),
        api.danhSachBanDich({ id }),
      ]);
      setSp(chiTiet);
      setBanDich(dich);
    } catch (ex) {
      if (!laChuaDangNhap(ex)) {
        setLoi(await loiTiengViet(ex));
      }
    }
  }, [id]);

  useEffect(() => {
    void (async () => {
      await nap();
    })();
  }, [nap]);

  if (loi) {
    return (
      <main>
        <p className="loi">{loi}</p>
        <Link href="/san-pham">← Về danh sách</Link>
      </main>
    );
  }

  if (!sp) {
    return (
      <main>
        <div className="dang-tai" style={{ width: '18rem', height: '1.6rem' }} />
        <div className="dang-tai" />
        <div className="dang-tai" />
      </main>
    );
  }

  const nguon = banDich.find((t) => t.isSource);

  return (
    <main>
      <p className="phu" style={{ margin: 0 }}>
        <Link href="/san-pham">← Sản phẩm</Link>
      </p>
      <h1>{nguon?.title ?? 'Sản phẩm'}</h1>
      <p className="phu">
        {sp.productType}
        {sp.durationDays ? ` · ${sp.durationDays} ngày` : ''}
        {sp.lastModifiedAt
          ? ` · sửa lần cuối ${new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(sp.lastModifiedAt)}`
          : ''}
      </p>

      <DaiTrangThai sp={sp} />

      <nav className="hang" style={{ margin: '1.5rem 0 0.5rem' }}>
        {TAB.map(([ma, ten]) => (
          <button key={ma} className={tab === ma ? '' : 'phu'} onClick={() => setTab(ma)}>
            {ten}
          </button>
        ))}
      </nav>

      {tab === 'chung' && <TabChung sp={sp} napLai={nap} />}
      {tab === 'dich' && <TabDich sp={sp} banDich={banDich} napLai={nap} />}
      {tab === 'thi-truong' && <TabThiTruong sp={sp} napLai={nap} />}
      {tab === 'ngay' && <TabNgayKhoiHanh sp={sp} />}
    </main>
  );
}

/**
 * Bốn cổng chặn của checklist mở bán (docs/22 mục 5), hiện thành một dải.
 *
 * Không tô màu cho đẹp: mỗi ô chưa đạt là một câu trả lời cho "còn thiếu gì".
 * Ô thứ ba hay bị quên nhất — dịch xong mà chưa gán thị trường thì sản phẩm vẫn
 * không tồn tại với khách.
 */
function DaiTrangThai({ sp }: { sp: AdminProductDetail }) {
  const nguon = sp.translations.find((t) => t.isSource);
  const dich = sp.translations.filter((t) => !t.isSource);

  const buoc: [string, boolean][] = [
    ['Bản nguồn đã xuất bản', nguon?.status === 'PUBLISHED'],
    ['Đã dịch và còn hạn', dich.length > 0 && dich.every((t) => t.status === 'PUBLISHED' && !t.outdated)],
    ['Đã gán thị trường', sp.markets.length > 0],
    ['Đang bán', sp.markets.some((m) => m.published)],
  ];

  return (
    <div className="hang" style={{ marginTop: '0.75rem' }}>
      {buoc.map(([ten, xong]) => (
        <span key={ten} className={`nhan ${xong ? 'xanh' : 'vang'}`}>
          {xong ? '✔' : '○'} {ten}
        </span>
      ))}
    </div>
  );
}
