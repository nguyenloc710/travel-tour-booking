package vn.travel.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ApiApplication {

    static {
        // Ứng dụng luôn chạy ở UTC — mọi TIMESTAMPTZ lưu UTC (docs/11 mục 9).
        //
        // Không chỉ là chuyện quy ước: driver JDBC gửi múi giờ mặc định của JVM
        // sang Postgres lúc mở kết nối, và Postgres từ chối những tên cũ mà Java
        // vẫn chấp nhận — máy đặt "Asia/Saigon" thì kết nối chết ngay với
        // 'invalid value for parameter "TimeZone"'. Đặt ở đây nên đúng cho cả
        // bootRun, jar chạy trực tiếp, lẫn container.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
