.**使用方法：**
1.  clone到本地后，确保拥有maven，docker,docker compose.
2.  进入目录DouyinMall，`mvn clean package -DskipTests`.
3.  执行`docker compose -f docker-compose.env.yaml build`.
4.  执行`docker compose -f docker-compose.env.yaml up -d`.
5.  打开http://主机ip:8848/   ,将nacos目录里的nacos_config_export.zip导入配置。
6.  执行`docker compose -f docker-compose.service.yaml build`.
7.  执行`docker compose -f docker-compose.service.yaml up -d`.

**注意** 
1. 由于项目包含七个微服务和若干架构组件，需要保证内存在16g左右。
2. 如果拉取镜像太慢，更换镜像源加速。
