package vn.travel.booking.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ghép đường dẫn tương đối trong CSDL thành địa chỉ tải được.
 *
 * <p><b>Vì sao tồn tại một lớp cho một phép nối chuỗi.</b> ADR-011 mục 2 và
 * docs/12 mục 4.6 cấm lưu URL đầy đủ vào {@code media_asset.path}: làm thế là
 * đóng cứng nhà cung cấp kho vào từng dòng dữ liệu, và ngày đổi kho hoặc thêm
 * CDN sẽ cần một migration thay vì một biến môi trường. Nên phép ghép phải xảy
 * ra ở đâu đó, và nó phải xảy ra <b>đúng một chỗ</b> — rải nó ra mỗi repository
 * là mở đường cho ngày một chỗ quên đổi.
 *
 * <p><b>Đây là endpoint CÔNG KHAI, không phải endpoint nội bộ.</b> Hai thứ đó
 * tách biệt có chủ ý (docs/15 mục 2): địa chỉ này đi vào thân phản hồi cho
 * trình duyệt, còn địa chỉ nội bộ chỉ dùng khi backend tự gọi kho. Dùng nhầm
 * địa chỉ nội bộ ở đây thì trình duyệt của khách không tải được ảnh, và ở dev
 * thì hai giá trị trùng nhau nên lỗi ẩn hoàn toàn cho tới lúc triển khai.
 */
@Component
public class DiaChiKho {

    private final String goc;

    public DiaChiKho(@Value("${travel.storage.public-base-url}") String goc) {
        // Bỏ dấu gạch chéo cuối để phép ghép bên dưới không sinh ra "//".
        this.goc = goc.endsWith("/") ? goc.substring(0, goc.length() - 1) : goc;
    }

    /**
     * @param duongDanTuongDoi giá trị của {@code media_asset.path}, ví dụ
     *                         {@code tour/sapa-ruong-bac-thang-01.jpg}
     */
    public String diaChiCua(String duongDanTuongDoi) {
        if (duongDanTuongDoi == null || duongDanTuongDoi.isBlank()) {
            return null;
        }
        String p = duongDanTuongDoi.startsWith("/") ? duongDanTuongDoi.substring(1) : duongDanTuongDoi;
        return goc + "/" + p;
    }
}
