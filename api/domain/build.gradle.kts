// domain — quy tắc nghiệp vụ thuần.
//
// KHÔNG phụ thuộc Spring, JPA, Jackson, hay bất cứ thứ gì có annotation.
// Chỉ thư viện chuẩn Java. Thêm một dòng dependency vào đây là phá ràng buộc
// quan trọng nhất của tầng backend — archTest sẽ báo đỏ.

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
