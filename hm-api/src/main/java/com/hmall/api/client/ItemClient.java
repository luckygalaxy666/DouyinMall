package com.hmall.api.client;



import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.client.fallback.ItemClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@FeignClient(value = "item-service",configuration = DefaultFeignConfig.class,fallbackFactory = ItemClientFallback.class)
public interface ItemClient {

    @GetMapping("items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

    @PutMapping("items/stock/deduct")
    void  deductStock(List<OrderDetailDTO> detailDTOS);



    @GetMapping("items/{id}")
    ItemDTO queryItemById(@PathVariable("id") Long id);

    @PutMapping("items")
    void updateItem(@RequestBody ItemDTO item);
}
