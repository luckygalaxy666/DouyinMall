# 使用官方的 OpenJDK 基础镜像
FROM eclipse-temurin:11-jdk-alpine

# 设置工作目录
WORKDIR /app

# 将本地的 JAR 文件复制到容器中
COPY ./target/item-service-1.0.0-exec.jar item-service.jar

# 暴露应用程序运行的端口（根据实际情况修改）
EXPOSE 8084

# 启动应用程序
ENTRYPOINT ["java","-Xmx256m", "-Xms256m", "-jar", "item-service.jar"]
