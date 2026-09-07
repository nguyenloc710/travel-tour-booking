// Một module Gradle, chia theo feature ở tầng package — ADR-010.
//
// Trước đây là bốn module (domain, application, infrastructure, web) với ranh
// giới phụ thuộc kiểm bằng ArchUnit. Đổi sang khuôn của dự án
// comic-social-network-be để hai dự án cùng người bảo trì đọc giống nhau; cái
// mất và cái được ghi ở ADR-010.

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi.generator)
}

group = "vn.travel"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
    }
}

dependencies {
    // BOM của Spring Boot quản phiên bản; không dùng plugin
    // spring-dependency-management, chỉ cần platform() là đủ.
    implementation(platform(libs.spring.boot.dependencies))
    annotationProcessor(platform(libs.spring.boot.dependencies))
    testImplementation(platform(libs.spring.boot.dependencies))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.flyway)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.jsr310)

    // Khoá job nền, dùng chính Postgres — docs/14 mục 6.4.
    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.jdbc)

    // Entity sang DTO luôn qua mapper sinh lúc biên dịch, không map tay.
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    // Thiếu dòng này thì Gradle tự chèn junit-platform-launcher lệch phiên bản
    // với engine do BOM quản, và JUnit chết ở bước dò test.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Test chạy ở UTC. Máy đặt múi giờ "Asia/Saigon" thì driver JDBC gửi đúng
    // chuỗi đó sang Postgres, và Postgres chỉ biết "Asia/Ho_Chi_Minh" — kết nối
    // bị từ chối ngay, không phải lỗi lược đồ.
    jvmArgs("-Duser.timezone=UTC")
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// ---------------------------------------------------------------- spec-first
//
// ADR-002 không đổi: contracts/openapi.yaml vẫn là nguồn sự thật, controller vẫn
// `implements` interface sinh ra, và đổi spec mà quên sửa controller vẫn là lỗi
// biên dịch.

val contractsFile = rootProject.file("../contracts/openapi.yaml")
val generatedDir = layout.buildDirectory.dir("generated/openapi")

val generateApiInterfaces by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("spring")
    inputSpec.set(contractsFile.absolutePath)
    outputDir.set(generatedDir.map { it.asFile.absolutePath })

    apiPackage.set("vn.travel.booking.web.generated.api")
    modelPackage.set("vn.travel.booking.web.generated.model")

    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "openApiNullable" to "false",
            "annotationLibrary" to "none",
            "documentationProvider" to "none",
            "useJakartaEe" to "true",
        )
    )
    globalProperties.set(mapOf("apis" to "", "models" to "", "supportingFiles" to "false"))
}

sourceSets["main"].java.srcDir(generatedDir.map { it.dir("src/main/java") })

tasks.named("compileJava") { dependsOn(generateApiInterfaces) }

// `pnpm contracts:generate` gọi task này ở phía Java.
tasks.register("contractsGenerate") {
    group = "openapi"
    description = "Sinh interface Java từ contracts/openapi.yaml"
    dependsOn(generateApiInterfaces)
}
