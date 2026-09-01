plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi.generator)
}

// web — controller và ánh xạ DTO. Module duy nhất dựng được ảnh chạy.
//
// Controller `implements` interface SINH RA từ contracts/openapi.yaml (ADR-002).
// Hệ quả: đổi spec mà quên sửa controller là lỗi biên dịch, không phải bug lúc
// chạy. Đó là lý do duy nhất để chọn spec-first thay vì code-first.

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    runtimeOnly(project(":infrastructure"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.jdbc)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(project(":infrastructure"))
}

// ---------------------------------------------------------------- spec-first

val contractsFile = rootProject.file("../contracts/openapi.yaml")
val generatedDir = layout.buildDirectory.dir("generated/openapi")

val generateApiInterfaces by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("spring")
    inputSpec.set(contractsFile.absolutePath)
    outputDir.set(generatedDir.map { it.asFile.absolutePath })

    apiPackage.set("vn.travel.booking.web.generated.api")
    modelPackage.set("vn.travel.booking.web.generated.model")

    // interfaceOnly: chỉ sinh interface, không sinh controller rỗng.
    // annotationLibrary/documentationProvider = none: không kéo theo swagger.
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
