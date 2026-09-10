package vn.travel.booking.media.dto;

import java.util.List;
import java.util.UUID;

/**
 * Một ảnh, nhìn từ trang quản trị.
 *
 * <p>{@code url} và {@code translatedLocales} là hai giá trị <b>tính ra</b>, và
 * chúng được tính ở tầng nghiệp vụ chứ không ở frontend: ghép URL là quy tắc của
 * backend (ADR-011 mục 2), còn "ảnh này vô hình ở locale nào" là hệ quả của luật
 * không fallback. Chép hai quy tắc đó sang frontend là để chúng lệch nhau.
 */
public record MediaView(
        UUID id,
        String kind,
        String path,
        String url,
        int width,
        int height,
        long byteSize,
        String source,
        String licenceRef,
        String contentType,
        Integer durationSeconds,
        String posterUrl,
        List<String> translatedLocales) {
}
