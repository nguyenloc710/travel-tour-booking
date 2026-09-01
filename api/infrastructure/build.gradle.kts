// infrastructure — hiện thực các cổng do application khai báo.
//
// JPA cho CRUD của trang quản trị; SQL thuần cho truy vấn listing của website
// khách, vì join bảng dịch kèm lọc theo market và sắp theo collation là chỗ JPA
// sinh SQL tệ (docs/10 mục 6).
//
// Migration Flyway nằm ở src/main/resources/db/migration.

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.data.jpa)

    // MapStruct sinh mapper lúc biên dịch: entity sang bản ghi của tầng
    // application. Không map tay trong service — quên một trường thì lỗi hiện
    // ra lúc chạy, còn để mapper sinh thì nó không quên.
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    implementation(libs.spring.boot.flyway)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
}
