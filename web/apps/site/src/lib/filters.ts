import { ProductSort, ProductType } from '@travel/api-client';

/**
 * Bộ lọc listing, đọc từ URL.
 *
 * Toàn bộ trạng thái bộ lọc nằm trong URL chứ không trong `useState`: F5 giữ
 * nguyên bộ lọc, nút Back hoạt động, và dán link cho người khác thì họ thấy
 * đúng kết quả đó (web/CLAUDE.md mục 5.2).
 */
export interface BoLoc {
  region?: string;
  productType?: string;
  q?: string;
  sort: string;
  page: number;
  size: number;
}

export const SIZE_MAC_DINH = 12;

/** Giá trị hợp lệ lấy thẳng từ enum sinh ra — thêm loại mới trong spec là tự có ở đây. */
const LOAI_HOP_LE: readonly string[] = Object.values(ProductType);
const SAP_XEP_HOP_LE: readonly string[] = Object.values(ProductSort);

export function docBoLoc(thamSo: Record<string, string | string[] | undefined>): BoLoc {
  return {
    region: chuoi(thamSo.region),
    // Giá trị rác trong URL bị BỎ QUA chứ không làm hỏng trang: người ta sửa
    // tay thanh địa chỉ, và trang trắng vì ?productType=abc là phản ứng quá đáng.
    productType: trongDanhSach(chuoi(thamSo.productType), LOAI_HOP_LE),
    q: chuoi(thamSo.q),
    sort: trongDanhSach(chuoi(thamSo.sort), SAP_XEP_HOP_LE) ?? ProductSort.Titleasc,
    page: soNguyen(thamSo.page, 0, 0),
    size: SIZE_MAC_DINH,
  };
}

/** Dựng lại chuỗi truy vấn, giữ nguyên mọi bộ lọc và chỉ đổi những gì cần đổi. */
export function chuoiTruyVan(boLoc: BoLoc, ghiDe: Partial<BoLoc> = {}): URLSearchParams {
  const gop = { ...boLoc, ...ghiDe };
  const thamSo = new URLSearchParams();
  if (gop.region) thamSo.set('region', gop.region);
  if (gop.productType) thamSo.set('productType', gop.productType);
  if (gop.q) thamSo.set('q', gop.q);
  if (gop.sort !== ProductSort.Titleasc) thamSo.set('sort', gop.sort);
  if (gop.page > 0) thamSo.set('page', String(gop.page));
  return thamSo;
}

function chuoi(gia_tri: string | string[] | undefined): string | undefined {
  const mot = Array.isArray(gia_tri) ? gia_tri[0] : gia_tri;
  const cat = mot?.trim();
  return cat ? cat : undefined;
}

function trongDanhSach(gia_tri: string | undefined, hopLe: readonly string[]): string | undefined {
  return gia_tri !== undefined && hopLe.includes(gia_tri) ? gia_tri : undefined;
}

function soNguyen(gia_tri: string | string[] | undefined, macDinh: number, toiThieu: number): number {
  const so = Number(chuoi(gia_tri));
  return Number.isInteger(so) && so >= toiThieu ? so : macDinh;
}
