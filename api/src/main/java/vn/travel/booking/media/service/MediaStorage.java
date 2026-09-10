package vn.travel.booking.media.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Kho ảnh — ký URL tải lên, và kiểm một tệp đã thật sự nằm trong kho chưa.
 *
 * <p><b>Hai client, hai địa chỉ, và đó là điểm dễ sai nhất ở đây.</b> Chữ ký
 * SigV4 bao gồm cả host, nên URL ký cho trình duyệt phải ký bằng <b>địa chỉ công
 * khai</b>; còn khi backend tự gọi kho thì nó đi bằng <b>địa chỉ nội bộ</b>.
 * ADR-011 mục 4 và docs/15 mục 2 chốt cả hai. Ở máy dev hai giá trị trùng nhau,
 * nên nhầm chúng <b>không lộ ra cho tới lúc triển khai</b> — lúc đó trình duyệt
 * nhận 403 và không nói vì sao.
 *
 * <p>Dùng SDK riêng của MinIO chứ không phải thư viện S3 tổng quát — ADR-013
 * sửa lại lựa chọn của ADR-011 mục 4, cái được và cái mất ghi ở đó.
 */
@Component
public class MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(MediaStorage.class);

    /** Đuôi tệp theo kiểu nội dung. Danh mục đóng, khớp enum trong hợp đồng. */
    private static String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/avif" -> "avif";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            default -> throw new IllegalArgumentException("kiểu nội dung ngoài danh mục: " + contentType);
        };
    }

    /**
     * Trần byte theo loại tệp — ADR-013, V9.
     *
     * <p>Hai trần chứ không một: một ảnh 10 MB là ảnh chưa ai nén, còn một video
     * 10 MB thì chỉ dài vài giây. Kiểm ở bước KÝ chứ không ở bước ghi bản ghi —
     * ký một URL là hứa nhận từng ấy byte vào đĩa, và sau khi tệp đã nằm trên đĩa
     * thì từ chối nó chỉ còn là dọn rác.
     */
    public static long maxBytesFor(String contentType) {
        return contentType.startsWith("video/") ? 200L * 1024 * 1024 : 10L * 1024 * 1024;
    }

    private final MinioClient signing;
    private final MinioClient internal;
    private final String bucket;
    private final Duration uploadUrlTtl;

    public MediaStorage(
            @Value("${travel.storage.endpoint-public}") String endpointPublic,
            @Value("${travel.storage.endpoint-internal}") String endpointInternal,
            @Value("${travel.storage.access-key}") String accessKey,
            @Value("${travel.storage.secret-key}") String secretKey,
            @Value("${travel.storage.bucket}") String bucket,
            @Value("${travel.storage.upload-url-ttl}") Duration uploadUrlTtl) {

        this.signing = MinioClient.builder()
                .endpoint(endpointPublic).credentials(accessKey, secretKey).build();
        this.internal = MinioClient.builder()
                .endpoint(endpointInternal).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
        this.uploadUrlTtl = uploadUrlTtl;
    }

    /**
     * Đường dẫn trong kho, <b>do máy chủ đặt</b>.
     *
     * <p>Tên tệp người dùng gửi lên chỉ dùng để lấy đuôi. Cho client chọn đường
     * dẫn là cho nó ghi đè ảnh của bản ghi khác — và đó không phải lỗ hổng cần
     * kẻ tấn công, chỉ cần hai người cùng tải lên tệp tên {@code anh.jpg}.
     */
    public String newObjectKey(String folder, String contentType) {
        return "%s/%s.%s".formatted(folder, UUID.randomUUID(), extensionOf(contentType));
    }

    /**
     * URL ký cho trình duyệt {@code PUT} thẳng tệp lên.
     *
     * <p>Chữ ký ràng buộc <b>phương thức, đường dẫn và hạn dùng</b> — không ràng
     * buộc nội dung tệp. Nghĩa là bước hai ({@code POST /admin/media}) không
     * được tin những gì client khai về tệp: nó phải hỏi lại kho, xem
     * {@link #stat(String)}.
     */
    public String presignPut(String objectKey) {
        try {
            return signing.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry((int) uploadUrlTtl.toSeconds())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("không ký được URL tải lên cho " + objectKey, e);
        }
    }

    public Duration uploadUrlTtl() {
        return uploadUrlTtl;
    }

    /**
     * Tệp có thật trong kho không, và nếu có thì bao nhiêu byte.
     *
     * <p>Đây là chỗ chặn bản ghi trỏ vào hư không: client hoàn toàn có thể gọi
     * bước hai mà chưa bao giờ tải tệp lên — vì lỗi mạng, vì đóng tab giữa
     * chừng, hoặc vì gọi thẳng API. Một dòng {@code media_asset} trỏ vào tệp
     * không tồn tại sinh ra ảnh vỡ trên trang khách, và không có gì báo.
     *
     * @return rỗng nếu kho nói không có tệp đó
     */
    public Optional<StatObjectResponse> stat(String objectKey) {
        try {
            return Optional.of(internal.statObject(StatObjectArgs.builder()
                    .bucket(bucket).object(objectKey).build()));
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() == null ? "" : String.valueOf(e.errorResponse().code());
            if (code.toLowerCase(Locale.ROOT).contains("nosuchkey")) {
                return Optional.empty();
            }
            throw new IllegalStateException("kho ảnh trả lỗi khi tra " + objectKey + ": " + code, e);
        } catch (Exception e) {
            // Kho không trả lời được là sự cố hạ tầng, không phải lỗi của người
            // nhập liệu — để nó nổi lên thành 500 kèm traceId.
            log.error("không tra được {} trong kho ảnh", objectKey, e);
            throw new IllegalStateException("không tra được " + objectKey + " trong kho ảnh", e);
        }
    }
}
