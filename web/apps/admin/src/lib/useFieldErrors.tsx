'use client';

import { useState, type ReactNode } from 'react';
import { fieldErrors } from './api';

/**
 * Lỗi từng trường, gắn vào đúng ô nhập của một biểu mẫu.
 *
 * API trả `fields` với đường dẫn trong **thân yêu cầu** (`source.longDescription`,
 * `groupTour.minPax`), còn biểu mẫu thì đánh dấu ô nhập bằng `id`. Hook này là
 * chỗ nối hai thứ đó, và nó ở đây thay vì chép vào từng màn hình vì cả ba việc
 * — dịch đường dẫn, tô dòng, nhảy tới ô sai — đều giống nhau ở mọi biểu mẫu.
 *
 * Dùng:
 * ```tsx
 * const O_THEO_DUONG_DAN = { 'source.title': 'tieu-de' };   // ngoài component
 * const loiO = useFieldErrors(O_THEO_DUONG_DAN);
 * // trong hàm gửi:  loiO.clear() trước, await loiO.show(ex) trong catch
 * // dưới mỗi ô:     {loiO.errorFor('tieu-de')}
 * ```
 */
export type FieldErrorBinding = {
  /** Xoá dấu — gọi trước mỗi lần gửi, nếu không thì lỗi cũ ở lại sau khi đã sửa. */
  clear: () => void;
  /** Đọc `fields` từ ngoại lệ, tô dòng, rồi nhảy tới ô sai đầu tiên trên trang. */
  show: (ex: unknown) => Promise<void>;
  /** Đặt ngay dưới ô nhập. Không có lỗi cho ô đó thì không hiện gì. */
  errorFor: (inputId: string) => ReactNode;
};

export function useFieldErrors(inputIdByPath: Record<string, string> = {}): FieldErrorBinding {
  const [errorByInput, setErrorByInput] = useState<Record<string, string>>({});

  function inputIdForPath(path: string): string {
    // Mặc định là đoạn cuối của đường dẫn: `groupTour.minPax` → ô `minPax`.
    // Biểu mẫu nào đặt id tiếng Việt thì khai trong bảng truyền vào.
    return inputIdByPath[path] ?? path.split('.').pop() ?? path;
  }

  return {
    clear: () => setErrorByInput({}),

    show: async (ex: unknown) => {
      const byInput: Record<string, string> = {};
      for (const [path, sentence] of Object.entries(await fieldErrors(ex))) {
        byInput[inputIdForPath(path)] = sentence;
      }
      setErrorByInput(byInput);

      // Nhảy tới ô sai ĐẦU TIÊN TRÊN TRANG, không phải ô đầu tiên máy chủ liệt
      // kê: thứ tự của `fields` là thứ tự kiểm tra ở backend, còn người dùng đọc
      // biểu mẫu từ trên xuống. Biểu mẫu tạo sản phẩm dài hơn một màn hình nên
      // chênh nhau là cuộn nhầm chỗ.
      const badInputs = Object.keys(byInput)
        .map((id) => document.getElementById(id))
        .filter((el): el is HTMLElement => el !== null)
        .sort((a, b) =>
          a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1,
        );
      badInputs[0]?.scrollIntoView({ block: 'center', behavior: 'smooth' });
      badInputs[0]?.focus({ preventScroll: true });
    },

    errorFor: (inputId: string) =>
      errorByInput[inputId] ? (
        <p className="loi" style={{ margin: '0.4rem 0 0' }}>
          {errorByInput[inputId]}
        </p>
      ) : null,
  };
}
