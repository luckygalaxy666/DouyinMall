FROM docker.elastic.co/elasticsearch/elasticsearch:7.12.1

# 设置工作目录
WORKDIR /usr/share/elasticsearch

# 下载并安装 IK 分词器插件
RUN ./bin/elasticsearch-plugin install --batch https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-7.12.1.zip && \
    # 清理缓存以减小镜像体积
    rm -rf /usr/share/elasticsearch/plugins/.installing