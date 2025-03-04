package com.hmall.api.client;

import com.hmall.api.client.fallback.TradeClientFallback;
import com.hmall.api.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;


@FeignClient(value = "trade-service",configuration = DefaultFeignConfig.class,fallbackFactory = TradeClientFallback.class)
public interface TradeClient {
    @PutMapping("orders/{orderId}")
    public void markOrderPaySuccess(@PathVariable("orderId") Long orderId);
}
