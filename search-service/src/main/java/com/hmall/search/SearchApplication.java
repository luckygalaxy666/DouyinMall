package com.hmall.search;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.search.config.ElasticsearchConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@EnableFeignClients(basePackages = "com.hmall.api.client",defaultConfiguration = DefaultFeignConfig.class)
@SpringBootApplication(scanBasePackages = {"com.hmall.search", "com.hmall.item"})
@MapperScan("com.hmall.item.mapper")
@EnableConfigurationProperties(ElasticsearchConfig.class) // 可选，视情况而定
public class SearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
