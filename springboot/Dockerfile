# ========== 阶段 1：把 fat jar 按"层"拆开 ==========
# 这个阶段只负责解包，不需要 Maven —— 因为 jar 是本地已经打好的
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /builder

# 用通配符，以后改版本号不用动 Dockerfile
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar

# Spring Boot 3.3+ 的 tools jarmode，按层提取
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ========== 阶段 2：运行（只带运行所需） ==========
FROM eclipse-temurin:21-jre-alpine

# ⚠️ 必须是 /app —— compose 里挂的是 /app/files
WORKDIR /app

# 按"变化频率从低到高"依次 COPY，每层独立缓存
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "application.jar"]