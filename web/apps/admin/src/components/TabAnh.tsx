'use client';

import { useCallback } from 'react';
import { MediaUploadUrlRequestFolderEnum, type AdminProductDetail } from '@travel/api-client';
import { adminApi } from '@/lib/api';
import { MediaList } from './MediaList';

/**
 * Bộ ảnh của sản phẩm — docs/05 mục 6.1 gọi nó là khối `[bộ ảnh]`.
 *
 * **Chỉ ảnh, không video.** Bảng nối là `product_image` và V9 cố tình không đổi
 * tên nó; máy chủ từ chối video ở đây. Video là chuyện của điểm đến, nơi mỗi
 * chặng dừng có bộ media riêng.
 */
export function TabAnh({ sp }: { sp: AdminProductDetail }) {
  const nap = useCallback(
    async () => (await adminApi().getProductImages({ id: sp.id })).items,
    [sp.id],
  );
  const luu = useCallback(
    async (assetIds: string[]) =>
      (await adminApi().saveProductImages({ id: sp.id, mediaOrder: { assetIds } })).items,
    [sp.id],
  );

  return (
    <MediaList
      nap={nap}
      luu={luu}
      folder={MediaUploadUrlRequestFolderEnum.Tour}
      tieuDe="Bộ ảnh của tour"
    />
  );
}
