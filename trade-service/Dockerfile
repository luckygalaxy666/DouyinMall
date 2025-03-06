# 使用官方的 OpenJDK 基础镜像
FROM openjdk:11-jre-slim

# 设置工作目录
WORKDIR /app

# 将本地的 JAR 文件复制到容器中
COPY trade-service.jar trade-service.jar

# 暴露应用程序运行的端口（根据实际情况修改）
EXPOSE 8085

# 启动应用程序
ENTRYPOINT ["java", "-jar", "trade-service.jar"]