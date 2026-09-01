plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.openapi.generator) apply false
}

// Bắt ở đây vì bên trong subprojects {} không truy cập được catalog `libs`.
val phienBanJava = libs.versions.java.get().toInt()

// ---------------------------------------------------------------- chung

allprojects {
    group = "vn.travel"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(phienBanJava))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // Test chạy ở UTC. Máy đặt múi giờ "Asia/Saigon" thì driver JDBC gửi
        // đúng chuỗi đó sang Postgres, và Postgres chỉ biết "Asia/Ho_Chi_Minh" —
        // kết nối bị từ chối ngay, không phải lỗi lược đồ. Dự án lưu UTC nên
        // test cũng chạy UTC.
        jvmArgs("-Duser.timezone=UTC")
        testLogging {
            events("failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

// ---------------------------------------------------------------- archTest
//
// Bài test ranh giới module nằm ở project gốc vì đây là nơi duy nhất nhìn thấy
// cả bốn module. `domain` không được biết Spring tồn tại — ràng buộc quan trọng
// nhất của tầng backend (docs/10 mục 3), và nó phải được kiểm tự động chứ không
// dựa vào kỷ luật cá nhân.

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(phienBanJava))
    }
}

dependencies {
    testImplementation(project(":domain"))
    testImplementation(project(":application"))
    testImplementation(project(":infrastructure"))
    testImplementation(project(":web"))
    testImplementation(libs.archunit.junit5)
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    // Không có dòng này thì Gradle tự chèn junit-platform-launcher phiên bản
    // khác với engine do BOM của Spring Boot quản, và JUnit chết ở bước dò test
    // với thông báo "OutputDirectoryCreator not available".
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    description = "Kiểm ranh giới phụ thuộc giữa các module"
    // Cấu hình trong subprojects {} không áp cho project gốc — thiếu dòng này
    // thì Gradle dùng JUnit 4 và lặng lẽ không tìm thấy test nào.
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

tasks.register("archTest") {
    group = "verification"
    description = "Kiểm ranh giới module — domain không được dính Spring"
    dependsOn(tasks.named("test"))
}
