'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import type { AdminDestination } from '@travel/api-client';
import { adminApi, laChuaDangNhap, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { LOAI, TRUONG_THEO_LOAI, khoiGui } from '@/lib/loaiSanPham';

/**
 * Tạo sản phẩm mới (docs/22 mục 5, bước 1–3).
 *
 * **Một biểu mẫu tạo ba thứ**, vì máy chủ nhận cả ba trong một lời gọi: dòng sản
 * phẩm, khối riêng của loại, và bản dịch ngôn ngữ nguồn. Không tách được — ràng
 * buộc `ct_product_source_translation` không cho một sản phẩm tồn tại mà thiếu
 * bản dịch nguồn, và cột của bảng con thì `NOT NULL`.
 *
 * Vì thế màn hình này dài. Tạo trước rồi điền sau là thứ **không làm được**,
 * không phải thứ chưa làm.
 */
export default function TaoSanPham() {
  const router = useRouter();
  const [diemDen, setDiemDen] = useState<AdminDestination[] | null>(null);

  const [productType, setProductType] = useState('GROUP_TOUR');
  const [primaryDestinationId, setPrimaryDestinationId] = useState('');
  const [durationDays, setDurationDays] = useState('14');
  const [heroImage, setHeroImage] = useState('');
  const [khoi, setKhoi] = useState<Record<string, string>>({});

  const [slug, setSlug] = useState('');
  const [title, setTitle] = useState('');
  const [shortDescription, setShortDescription] = useState('');
  const [longDescription, setLongDescription] = useState('');
  const [whyChooseThis, setWhyChooseThis] = useState('');
  const [heroImageAlt, setHeroImageAlt] = useState('');

  const [loi, setLoi] = useState('');
  const [dangGui, setDangGui] = useState(false);

  useEffect(() => {
    let conHieuLuc = true;
    void (async () => {
      try {
        const ds = await adminApi().danhSachDiemDenQuanTri();
        if (conHieuLuc) {
          setDiemDen(ds);
        }
      } catch (ex) {
        if (conHieuLuc && !laChuaDangNhap(ex)) {
          setLoi(await loiTiengViet(ex));
        }
      }
    })();
    return () => {
      conHieuLuc = false;
    };
  }, []);

  const laDayTour = productType === 'DAY_TOUR';
  const truong = TRUONG_THEO_LOAI[productType] ?? [];
  const slugCoDau = slug !== '' && /[^a-z0-9-]/.test(slug);
  const soY = whyChooseThis.split('\n').filter((d) => d.trim()).length;

  async function gui(e: React.FormEvent) {
    e.preventDefault();
    setLoi('');
    setDangGui(true);
    try {
      const sp = await adminApi().taoSanPham({
        adminProductCreate: {
          productType: productType as never,
          primaryDestinationId,
          // DAY_TOUR không có số ngày, mọi loại khác thì bắt buộc — ràng buộc
          // CSDL cưỡng chế cả hai chiều, nên biểu mẫu cũng phải theo cả hai.
          durationDays: laDayTour ? undefined : Number(durationDays),
          heroImage,
          source: {
            slug,
            title,
            shortDescription,
            longDescription: longDescription
              .split(/\n\s*\n/)
              .map((d) => d.trim())
              .filter(Boolean),
            whyChooseThis: whyChooseThis
              .split('\n')
              .map((d) => d.trim())
              .filter(Boolean),
            heroImageAlt,
            // Tạo ra ở trạng thái nháp. Nó CHƯA vào hàng đợi dịch cho tới khi
            // được xuất bản — docs/22 mục 4.1.
            status: 'DRAFT',
          },
          ...khoiGui(productType, khoi),
        },
      });
      router.replace(`/san-pham/${sp.id}`);
    } catch (ex) {
      setLoi(
        laKhongDuQuyen(ex)
          ? 'Chỉ vai trò EDITOR hoặc ADMIN tạo được sản phẩm.'
          : await loiTiengViet(ex),
      );
    } finally {
      setDangGui(false);
    }
  }

  return (
    <main>
      <p className="phu" style={{ margin: 0 }}>
        <Link href="/san-pham">← Sản phẩm</Link>
      </p>
      <div className="dau-trang">
        <div>
          <h1>Tour mới</h1>
          <p className="phu">
            Tạo xong là sản phẩm ở trạng thái nháp: chưa dịch, chưa gán thị trường,
            chưa có ngày khởi hành — nên khách chưa thấy gì. Các bước còn lại làm ở
            màn hình sửa.
          </p>
        </div>
      </div>

      <form onSubmit={gui}>
        <div className="the">
          <h2 style={{ marginTop: 0 }}>Loại và điểm đến</h2>

          <label htmlFor="loai">Loại sản phẩm</label>
          <select
            id="loai"
            value={productType}
            onChange={(e) => {
              setProductType(e.target.value);
              // Đổi loại là đổi hẳn bộ trường; giữ giá trị cũ lại là gửi lên
              // những trường không thuộc loại mới.
              setKhoi({});
            }}
          >
            {LOAI.filter(([ma]) => ma).map(([ma, ten]) => (
              <option key={ma} value={ma}>
                {ten}
              </option>
            ))}
          </select>
          <p className="phu" style={{ marginTop: '0.25rem' }}>
            <strong>Không đổi được sau khi tạo.</strong> Mỗi loại một bộ trường
            riêng; đổi loại là mất dữ liệu của loại cũ.
          </p>

          <label htmlFor="dd">Điểm đến chính</label>
          {diemDen === null ? (
            <div className="dang-tai" style={{ maxWidth: '32rem' }} />
          ) : (
            <select
              id="dd"
              required
              value={primaryDestinationId}
              onChange={(e) => setPrimaryDestinationId(e.target.value)}
            >
              <option value="">— chọn điểm đến —</option>
              {diemDen.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} · {d.regionName}
                </option>
              ))}
            </select>
          )}

          {!laDayTour && (
            <>
              <label htmlFor="so-ngay">Số ngày</label>
              <input
                id="so-ngay"
                type="number"
                min={1}
                max={60}
                required
                value={durationDays}
                onChange={(e) => setDurationDays(e.target.value)}
                style={{ maxWidth: '8rem' }}
              />
            </>
          )}

          <label htmlFor="anh">Ảnh đầu trang</label>
          <input
            id="anh"
            required
            placeholder="/img/ten-anh.jpg"
            value={heroImage}
            onChange={(e) => setHeroImage(e.target.value)}
          />
          <p className="phu" style={{ marginTop: '0.25rem' }}>
            Đường dẫn nhập tay. Tải ảnh lên chưa có — chờ ADR-008 chốt nơi lưu.
          </p>
        </div>

        {truong.length > 0 && (
          <div className="the" style={{ marginTop: '1.25rem' }}>
            <h2 style={{ marginTop: 0 }}>Phần riêng của loại</h2>
            {truong.map(([ten, nhan, kieu]) => (
              <div key={ten}>
                <label htmlFor={ten}>{nhan}</label>
                <input
                  id={ten}
                  required
                  type={kieu === 'number' ? 'number' : 'text'}
                  value={khoi[ten] ?? ''}
                  onChange={(e) => setKhoi({ ...khoi, [ten]: e.target.value })}
                  style={{ maxWidth: kieu === 'number' ? '10rem' : '24rem' }}
                />
              </div>
            ))}
          </div>
        )}

        <div className="the" style={{ marginTop: '1.25rem' }}>
          <h2 style={{ marginTop: 0 }}>Nội dung tiếng Đan Mạch</h2>
          <p className="phu">
            Tiếng Đan là <strong>ngôn ngữ nguồn</strong> (ADR-004): nội dung viết
            bằng <code>da</code> trước, dịch sang <code>vi</code> sau. Không có chiều
            ngược lại, nên biểu mẫu này chỉ nhận bản <code>da</code>.
          </p>

          <label htmlFor="slug">Slug</label>
          <input
            id="slug"
            required
            placeholder="rundrejse-nord-til-syd"
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
          />
          {slugCoDau && (
            <p className="loi" style={{ margin: '0.4rem 0 0' }}>
              Slug chỉ được dùng a–z, 0–9 và dấu gạch nối — không dấu ở cả hai ngôn
              ngữ. <code>bekraeftelse</code> chứ không <code>bekræftelse</code>.
            </p>
          )}

          <label htmlFor="tieu-de">Tiêu đề</label>
          <input id="tieu-de" required value={title} onChange={(e) => setTitle(e.target.value)} />

          <label htmlFor="mo-ta">Mô tả ngắn</label>
          <textarea
            id="mo-ta"
            required
            value={shortDescription}
            onChange={(e) => setShortDescription(e.target.value)}
          />

          <label htmlFor="dai">Mô tả dài — cách nhau một dòng trống, ít nhất 2 đoạn</label>
          <textarea
            id="dai"
            required
            value={longDescription}
            onChange={(e) => setLongDescription(e.target.value)}
          />

          <label htmlFor="vi-sao">Vì sao chọn tour này — mỗi dòng một ý, 3 đến 7 ý</label>
          <textarea
            id="vi-sao"
            required
            value={whyChooseThis}
            onChange={(e) => setWhyChooseThis(e.target.value)}
          />
          {whyChooseThis !== '' && (soY < 3 || soY > 7) && (
            <p className="loi" style={{ margin: '0.4rem 0 0' }}>
              Đang có {soY} ý. Cơ sở dữ liệu chỉ nhận từ 3 tới 7.
            </p>
          )}

          <label htmlFor="alt">Chữ thay ảnh đầu trang</label>
          <input
            id="alt"
            required
            value={heroImageAlt}
            onChange={(e) => setHeroImageAlt(e.target.value)}
          />
          <p className="phu" style={{ marginTop: '0.25rem' }}>
            Đây là <strong>nội dung phải dịch</strong>, không phải siêu dữ liệu kỹ
            thuật — nó là thứ người khiếm thị đọc thay cho ảnh.
          </p>
        </div>

        {loi && <p className="loi">{loi}</p>}

        <div className="hang" style={{ marginTop: '1.25rem' }}>
          <button type="submit" disabled={dangGui}>
            {dangGui ? 'Đang tạo…' : 'Tạo tour'}
          </button>
          <Link href="/san-pham">
            <button type="button" className="phu">
              Huỷ
            </button>
          </Link>
        </div>
      </form>
    </main>
  );
}
