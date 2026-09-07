# syntax=docker/dockerfile:1

# Ảnh backend, hai giai đoạn — docs/34 mục 5.1.
#
# BỐI CẢNH BUILD LÀ GỐC REPO, không phải api/:
#
#     docker build -f api/Dockerfile -t travel-api .
#
# `build.gradle.kts` đọc `../contracts/openapi.yaml` để sinh interface mà
# controller `implements` (ADR-002). Lấy api/ làm bối cảnh thì file spec nằm
# ngoài tầm với, và build chết ở bước SINH CODE chứ không phải bước biên dịch —
# thông báo lỗi lúc đó không nhắc gì tới bối cảnh, rất khó đoán ra.

# ---------------------------------------------------------------- giai đoạn 1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

COPY api/gradlew ./api/
COPY api/gradle ./api/gradle
COPY api/settings.gradle.kts api/build.gradle.kts api/gradle.properties ./api/
COPY contracts ./contracts
COPY api/src ./api/src

WORKDIR /src/api

# Checkout từ Windows làm mất bit thực thi. Không có dòng này thì lỗi là
# "permission denied" ở dòng dưới.
RUN chmod +x gradlew

# `-x test` có chủ ý, hai lý do:
#   1. Test đã chạy ở `api.yml` TRƯỚC khi tới bước build ảnh — chạy lại là thừa.
#   2. Testcontainers cần Docker daemon, thứ không có bên trong `docker build`.
#      Để test chạy ở đây thì nó chết vì lý do không liên quan gì tới code.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test

# Tên file jar mang version của `build.gradle.kts` (0.1.0-SNAPSHOT). Đổi tên ở
# đây để giai đoạn sau không phải biết version — nâng version không kéo theo
# việc sửa Dockerfile. `grep -v plain` loại jar không chạy được của task `jar`.
RUN cp "$(ls build/libs/*.jar | grep -v -- '-plain' | head -1)" /app.jar

# ---------------------------------------------------------------- giai đoạn 2
#
# Ảnh cuối chỉ có JRE và một file jar: không Gradle, không JDK đầy đủ, không mã
# nguồn, không bí mật build — docs/34 mục 5.1.
FROM eclipse-temurin:21-jre AS runtime

# Không chạy bằng root. Ứng dụng không ghi gì vào hệ thống tệp của container:
# ảnh đi vào MinIO (ADR-011), dữ liệu đi vào Postgres.
RUN groupadd --system travel && useradd --system --gid travel --home /app travel

WORKDIR /app
COPY --from=build --chown=travel:travel /app.jar ./app.jar
USER travel

EXPOSE 8080

# Không đặt -Xmx cố định: container có bao nhiêu RAM là việc của `compose.prod`,
# JVM đọc giới hạn cgroup và tự tính. Đặt số cứng ở đây là chọn sai cho mọi máy
# trừ đúng một máy.
#
# Flyway chạy lúc ứng dụng khởi động, TRƯỚC khi cổng 8080 mở — docs/34 mục 4.
# Migration đỏ nghĩa là container không bao giờ nhận traffic, đúng như mong muốn.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
