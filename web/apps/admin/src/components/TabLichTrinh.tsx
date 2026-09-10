'use client';

import { useCallback, useEffect, useState } from 'react';
import type {
  AdminDestination,
  AdminItineraryDayInput,
  AdminProductDetail,
} from '@travel/api-client';
import { adminApi, laKhongDuQuyen, loiTiengViet } from '@/lib/api';
import { useFieldErrors } from '@/lib/useFieldErrors';

/**
 * Lịch trình từng ngày — docs/22 M3, `docs/05` mục 5.2.
 *
 * **Lưu cả mảng một lần**, không lưu từng ngày. Số ngày phải bằng `durationDays`
 * và `dayNumber` phải liền mạch từ 1 — hai luật chỉ kiểm được khi nhìn cả mảng
 * (quy tắc 3 của `docs/12` mục 9), nên máy chủ nhận cả mảng và màn hình này gửi
 * cả mảng.
 *
 * Nút **"Tạo N ngày trống"** không phải tiện ích cho vui: gõ tay mười lăm dòng
 * để rồi bị từ chối vì thiếu một dòng là chỗ người ta bỏ cuộc.
 */
export function TabLichTrinh({ sp }: { sp: AdminProductDetail }) {
  const [ngay, setNgay] = useState<AdminItineraryDayInput[] | null>(null);
  const [diemDen, setDiemDen] = useState<AdminDestination[]>([]);
  const [loi, setLoi] = useState('');
  const [xong, setXong] = useState('');
  const [dangLuu, setDangLuu] = useState(false);
  const loiO = useFieldErrors();

  const soNgayCanCo = sp.durationDays ?? 0;

  // `setLoi('')` nằm SAU `await` chứ không mở đầu hàm: gọi setState đồng bộ
  // ngay trong một effect làm React dựng lại một lượt thừa, và eslint chặn.
  const nap = useCallback(async () => {
    try {
      const [lich, ds] = await Promise.all([
        adminApi().getAdminItinerary({ id: sp.id }),
        adminApi().listAdminDestinations(),
      ]);
      setLoi('');
      setDiemDen(ds);
      setNgay(
        lich.days.map((d) => ({
          dayNumber: d.dayNumber,
          destinationId: d.destinationId,
          hotelId: d.hotelId,
          // Bản NGUỒN. Tab này sửa bản `da`; bản `vi` là việc của người dịch,
          // và nó có endpoint riêng để người dịch không đổi được cấu trúc.
          title: d.translations.find((t) => t.isSource)?.title ?? '',
          description: d.translations.find((t) => t.isSource)?.description ?? '',
        })),
      );
    } catch (ex) {
      setNgay([]);
      setLoi(await loiTiengViet(ex));
    }
  }, [sp.id]);

  // Bọc trong một hàm async chứ không `void nap()` trần — cùng khuôn với
  // TabNgayKhoiHanh. Gọi trần làm eslint coi như effect gọi setState đồng bộ.
  useEffect(() => {
    void (async () => {
      await nap();
    })();
  }, [nap]);

  function doiNgay(i: number, sua: Partial<AdminItineraryDayInput>) {
    setNgay((cu) => (cu === null ? cu : cu.map((d, j) => (j === i ? { ...d, ...sua } : d))));
  }

  function taoNgayTrong() {
    setNgay(
      Array.from({ length: soNgayCanCo }, (_, i) => ({
        dayNumber: i + 1,
        title: '',
        description: '',
      })),
    );
    setXong('');
  }

  async function luu() {
    if (ngay === null) {
      return;
    }
    setLoi('');
    setXong('');
    loiO.clear();
    setDangLuu(true);
    try {
      await adminApi().saveItinerary({
        id: sp.id,
        adminItinerarySave: { days: ngay },
      });
      setXong('Đã lưu lịch trình.');
      await nap();
    } catch (ex) {
      setLoi(laKhongDuQuyen(ex) ? 'Chỉ vai trò EDITOR hoặc ADMIN sửa được lịch trình.' : await loiTiengViet(ex));
      await loiO.show(ex);
    } finally {
      setDangLuu(false);
    }
  }

  if (ngay === null) {
    return <div className="dang-tai" style={{ height: '12rem' }} />;
  }

  return (
    <>
      <h2>Lịch trình từng ngày</h2>
      <p className="phu">
        Tour này dài <strong>{soNgayCanCo} ngày</strong>, nên lịch trình phải có
        đúng {soNgayCanCo} mục — máy chủ từ chối nếu lệch. Ngày bay hoặc ngày ngủ
        trên tàu thì để trống điểm đến và khách sạn; đó là trạng thái hợp lệ, không
        phải dữ liệu thiếu.
      </p>

      {ngay.length === 0 && (
        <div className="the" style={{ marginTop: '1rem' }}>
          <p style={{ marginTop: 0 }}>Chưa có lịch trình nào.</p>
          <button type="button" onClick={taoNgayTrong} disabled={soNgayCanCo === 0}>
            Tạo {soNgayCanCo} ngày trống
          </button>
        </div>
      )}

      {/* Lỗi của cả mảng — số ngày lệch durationDays rơi vào đây, không vào ô nào. */}
      {loiO.errorFor('days')}

      {ngay.map((d, i) => (
        <div className="the" key={d.dayNumber} style={{ marginTop: '1rem' }}>
          <h3 style={{ marginTop: 0 }}>Ngày {d.dayNumber}</h3>

          <label htmlFor={`lt-tieu-de-${i}`}>Tiêu đề — tiếng Đan</label>
          <input
            id={`lt-tieu-de-${i}`}
            value={d.title}
            onChange={(e) => doiNgay(i, { title: e.target.value })}
          />
          {loiO.errorFor(`days[${i}].title`)}

          <label htmlFor={`lt-mo-ta-${i}`}>Mô tả — tiếng Đan</label>
          <textarea
            id={`lt-mo-ta-${i}`}
            value={d.description}
            onChange={(e) => doiNgay(i, { description: e.target.value })}
          />
          {loiO.errorFor(`days[${i}].description`)}

          <label htmlFor={`lt-diem-den-${i}`}>Ngủ đêm ở</label>
          <select
            id={`lt-diem-den-${i}`}
            value={d.destinationId ?? ''}
            onChange={(e) => doiNgay(i, { destinationId: e.target.value || undefined })}
          >
            <option value="">— không ngủ ở điểm đến nào —</option>
            {diemDen.map((dd) => (
              <option key={dd.id} value={dd.id}>
                {dd.name} · {dd.regionName}
              </option>
            ))}
          </select>
          {loiO.errorFor(`days[${i}].destinationId`)}
          {loiO.errorFor(`days[${i}].dayNumber`)}
        </div>
      ))}

      {loi && <p className="loi">{loi}</p>}
      {xong && <p className="xong">{xong}</p>}

      {ngay.length > 0 && (
        <p style={{ marginTop: '1rem' }}>
          <button type="button" disabled={dangLuu} onClick={luu}>
            {dangLuu ? 'Đang lưu…' : 'Lưu lịch trình'}
          </button>
        </p>
      )}
    </>
  );
}
