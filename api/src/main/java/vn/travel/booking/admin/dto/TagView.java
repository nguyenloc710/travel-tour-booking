package vn.travel.booking.admin.dto;

import java.util.UUID;

/** Thẻ bài viết, tên ở ngôn ngữ nguồn. */
public record TagView(UUID id, String code, String name) {
}
