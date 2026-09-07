package vn.travel.booking.admin.dto;

/**
 * Một locale của một bản ghi nội dung đang ở đâu — docs/22 M13.
 *
 * <p>{@code MISSING} là giá trị <b>thứ tư</b>, không nằm trong CSDL: ba cái kia
 * là giá trị của cột {@code status}, còn cái này nghĩa là <b>không có dòng bản
 * dịch nào</b>. Gộp nó vào {@code DRAFT} cho gọn là xoá mất khác biệt duy nhất
 * mà màn hình cần nói ra: bài chưa ai đụng tới khác bài đang viết dở.
 */
public enum ContentLocaleState {
    MISSING,
    DRAFT,
    TRANSLATED,
    PUBLISHED
}
