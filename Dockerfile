# ==========================================
# STAGE 1: BUILD (Đóng gói ứng dụng)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy file cấu hình maven và source code vào container
COPY pom.xml .
COPY src ./src

# Chạy lệnh build ra file .jar (bỏ qua chạy Test để build nhanh hơn)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: RUN (Chạy ứng dụng)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build từ Stage 1 sang Stage 2
COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080 ra ngoài
EXPOSE 8080

# Lệnh khởi động Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
