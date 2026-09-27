# 用 Java 21 的 JRE 镜像：项目是 Java 21；容器只负责运行，
# 不需要 JDK 和 Maven，用 JRE 镜像能小几百 MB
FROM eclipse-temurin:21-jre

# 容器内的工作目录。FileController 把上传文件写在 user.dir/files，
# 也就是 /app/files —— 以后挂 volume 要挂这个路径
WORKDIR /app

# 把本地打好的 fat jar 拷进去并改名；用通配符，以后改版本号不用动这里
COPY target/*.jar app.jar

# 应用在 application.yml 里配的 server.port
EXPOSE 9090

# 启动命令；app.jar 必须和上面 COPY 的目标名一致
ENTRYPOINT ["java", "-jar", "app.jar"]