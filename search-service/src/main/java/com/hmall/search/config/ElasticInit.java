package com.hmall.search.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;

import com.hmall.common.utils.CollUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class ElasticInit {

    private final RestHighLevelClient client;
    private final IItemService itemService;

    @Autowired
    public ElasticInit(RestHighLevelClient client, IItemService itemService) {
        this.client = client;
        this.itemService = itemService;
    }

    @PostConstruct
    public void init() {
        try {
            if (!indexExists("items")) {
                createIndex("items");
                loadItemDocs();
            } else {
                log.info("索引 'items' 已存在，跳过创建和数据加载。");
            }
        } catch (IOException e) {
            log.error("初始化 Elasticsearch 索引和数据时出错", e);
        }
    }

    /**
     * 检查索引是否存在
     *
     * @param index 索引名称
     * @return 如果存在返回 true，否则返回 false
     */
    private boolean indexExists(String index) throws IOException {
        GetIndexRequest request = new GetIndexRequest(index);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * 创建索引
     *
     * @param index 索引名称
     * @throws IOException
     */
    private void createIndex(String index) throws IOException {
        // 1.创建Request对象
        CreateIndexRequest request = new CreateIndexRequest("items");
        // 2.准备请求参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3.发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    /**
     * 加载商品数据到 Elasticsearch
     */
    private void loadItemDocs() throws IOException {
        // 分页查询商品数据
        int pageNo = 1;
        int size = 1000;
        log.info("itemService={}, client={}", itemService, client);
        while (true) {
            Page<Item> page = itemService.lambdaQuery().eq(Item::getStatus, 1).page(new Page<Item>(pageNo, size));
            // 非空校验
            List<Item> items = page.getRecords();
            if (CollUtils.isEmpty(items)) {
                return;
            }
            log.info("加载第{}页数据，共{}条", pageNo, items.size());
            // 1.创建Request
            BulkRequest request = new BulkRequest("items");
            // 2.准备参数，添加多个新增的Request
            for (Item item : items) {
                // 2.1.转换为文档类型ItemDTO
                ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
                // 2.2.创建新增文档的Request对象
                request.add(new IndexRequest()
                        .id(itemDoc.getId())
                        .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON));
            }
            // 3.发送请求
            client.bulk(request, RequestOptions.DEFAULT);

            // 翻页
            pageNo++;
        }
    }

    static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

}