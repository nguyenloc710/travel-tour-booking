package vn.travel.booking.admin.dto;

import java.util.UUID;

/**
 * Một điểm đến để chọn trong ô {@code select} của trang quản trị.
 *
 * <p>Mang {@code id} chứ không mang {@code slug}: {@code POST /admin/products}
 * nhận id, và slug thì đổi được còn id thì không.
 */
public record DestinationOption(UUID id, String code, String name, String regionName) {
}
