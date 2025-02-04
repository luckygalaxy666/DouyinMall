package com.hmall.search.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;

import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.domain.query.ItemPageQuery;
import com.hmall.item.service.IItemService;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http://172.27.62.11:9200")
        ));
    private final ISearchService searchService;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        // 分页查询
//        Page<Item> result = searchService.lambdaQuery()
//                .like(StrUtil.isNotBlank(query.getKey()), Item::getName, query.getKey())
//                .eq(StrUtil.isNotBlank(query.getBrand()), Item::getBrand, query.getBrand())
//                .eq(StrUtil.isNotBlank(query.getCategory()), Item::getCategory, query.getCategory())
//                .eq(Item::getStatus, 1)
//                .between(query.getMaxPrice() != null, Item::getPrice, query.getMinPrice(), query.getMaxPrice())
//                .page(query.toMpPage("update_time", false));

        // 使用es查询
        log.info("使用ElasticSearch查询");
        SearchRequest searchRequest = new SearchRequest("items");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if(StrUtil.isNotBlank(query.getKey())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if(StrUtil.isNotBlank(query.getBrand())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("brand", query.getBrand()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("category", query.getCategory()));
        }
        if (query.getMinPrice() != null) {
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }
        if (query.getMaxPrice() != null) {
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }

        searchRequest.source().query(boolQueryBuilder);
        int pageNo = query.getPageNo();
        int size = query.getPageSize();
        // 分页查询
        searchRequest.source().from((pageNo - 1) * size).size(size);
        // 排序
        if(StrUtil.isNotBlank(query.getSortBy()))
            searchRequest.source().sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC :SortOrder.DESC);

        // 发送请求
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        // 封装并返回
        long total = response.getHits().getTotalHits().value;
        List<ItemDTO> list = handleResponse(response);



        return new PageDTO<ItemDTO>(total, (long)pageNo, list);
    }

    @ApiOperation("通过es根据id查询商品")
    @GetMapping("{id}")
    public ItemDTO queryItemById(@PathVariable("id") Long id) throws Exception {

        log.info("使用ElasticSearch根据id查询商品");
        // 1.创建Request
        GetRequest getRequest = new GetRequest("items", id.toString());
        // 2.发送请求
        GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
        // 3.解析响应
        if (!response.isExists()) {
            return null;
        }
        String sourceAsString = response.getSourceAsString();
        ItemDoc bean = JSONUtil.toBean(sourceAsString, ItemDoc.class);
        return BeanUtils.copyBean(bean, ItemDTO.class);
    }

    @ApiOperation("搜索商品分类")
    @PostMapping("/filters")
    public Map filters(@RequestBody ItemPageQuery query) throws IOException {

        log.info("使用ElasticSearch搜索商品分类");
        SearchRequest searchRequest = new SearchRequest("items");

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (StrUtil.isNotBlank(query.getKey())) {
            queryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StrUtil.isNotBlank(query.getBrand())) {
            queryBuilder.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            queryBuilder.filter(QueryBuilders.matchQuery("category", query.getCategory()));
        }
        if (query.getMaxPrice() != null) {
            queryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }

        String categoryAgg = "category_agg";
        String brandAgg = "brand_agg";
        searchRequest.source().query(queryBuilder).aggregation(
                        AggregationBuilders.terms(categoryAgg).field("category"))
                .aggregation(AggregationBuilders.terms(brandAgg).field("brand"));

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        HashMap<String, List<String>> resultMap = new HashMap<>();
        Terms terms = response.getAggregations().get(categoryAgg);
        if (terms != null) {
            resultMap.put("category",terms.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList()));
        }
        terms = response.getAggregations().get(brandAgg);
        if (terms != null) {
            resultMap.put("brand",terms.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList()));
        }
        // 封装并返回
        return resultMap;
    }

    private List<ItemDTO> handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 1.获取总条数
        long total = searchHits.getTotalHits().value;
//        System.out.println("共搜索到" + total + "条数据");
        // 2.遍历结果数组
        SearchHit[] hits = searchHits.getHits();
        List<ItemDTO> list = CollUtils.newArrayList();

        for (SearchHit hit : hits) {
            // 3.得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 4.反序列化并打印
            ItemDoc item = JSONUtil.toBean(source, ItemDoc.class);
            list.add(BeanUtils.copyBean(item, ItemDTO.class));
        }
        return list;
    }
}
