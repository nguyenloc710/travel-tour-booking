// application — use case, transaction, cổng ra ngoài.
//
// Được phụ thuộc domain. KHÔNG được phụ thuộc JPA hay Spring Web.
// Chỉ dùng phần lõi của Spring cho annotation transaction và stereotype.

dependencies {
    implementation(project(":domain"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-context")

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
}
