package com.hmall.search.listener;


import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.utils.BeanUtils;

import com.hmall.item.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchListener {


    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http://172.20.110.176:9200")
    ));

    private final ItemClient itemClient;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.delete.queue", durable = "true"),
            exchange = @Exchange(name = "es", type = ExchangeTypes.TOPIC),
            key = "item.delete"
    ))
    public void listenDeleteItem(Long id) throws IOException {
        // 1.创建Request
        DeleteRequest deleteRequest = new DeleteRequest("items", id.toString());
        // 2.发送请求
        client.delete(deleteRequest, RequestOptions.DEFAULT);
        log.info("es删除商品成功");

    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.update.queue", durable = "true"),
            exchange = @Exchange(name = "es", type = ExchangeTypes.TOPIC),
            key = "item.update"
    ))
    public void listenUpdateItem(Long id) throws IOException {
        // 1.创建Request
        IndexRequest indexRequest = common(id);
        // 2.发送请求
        client.index(indexRequest, RequestOptions.DEFAULT);
        log.info("es更新商品成功");

    }

    private IndexRequest common(Long id) {
        // 1.查询商品
        ItemDTO itemDTO = itemClient.queryItemById(id);
        // 2.转换为Doc
        ItemDoc itemDoc = BeanUtils.copyBean(itemDTO, ItemDoc.class);
        itemDoc.setUpdateTime(LocalDateTime.now());
        // 3.创建Request
        IndexRequest indexRequest = new IndexRequest("items");
        indexRequest.id(id.toString());
        indexRequest.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        return indexRequest;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.add.queue", durable = "true"),
            exchange = @Exchange(name = "es", type = ExchangeTypes.TOPIC),
            key = "item.add"
    ))
    public void listenAddItem(Long id) throws IOException {
        // 1.创建Request
        IndexRequest indexRequest = common(id);
        // 2.发送请求
        client.index(indexRequest, RequestOptions.DEFAULT);
        log.info("es新增商品成功");

    }


}
