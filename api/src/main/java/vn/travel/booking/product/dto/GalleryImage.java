package vn.travel.booking.product.dto;

/**
 * Một tấm trong bộ ảnh của sản phẩm.
 *
 * <p><b>{@code url} là địa chỉ đầy đủ, nhưng CSDL không lưu nó.</b>
 * {@code media_asset.path} giữ đường dẫn tương đối
 * ({@code tour/sapa-ruong-bac-thang-01.jpg}) và tầng đọc ghép nó với địa chỉ gốc
 * lấy từ cấu hình — ADR-011 mục 2, docs/12 mục 4.6. Lưu URL đầy đủ vào từng dòng
 * dữ liệu là đóng cứng nhà cung cấp vào dữ liệu, và ngày đổi kho sẽ cần một
 * migration thay vì một biến môi trường.
 *
 * <p>{@code width} và {@code height} là kích thước <b>ảnh gốc</b>, không phải
 * kích thước hiển thị. Chúng đi kèm để frontend đặt chỗ trước cho ảnh; thiếu
 * chúng thì trang nhảy một nhịp khi ảnh tải xong, và ở trang có bộ ảnh dài thì
 * nhảy nhiều lần.
 */
public record GalleryImage(
        String url,
        String alt,
        int width,
        int height) {
}
