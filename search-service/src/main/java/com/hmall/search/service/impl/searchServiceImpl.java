package com.hmall.search.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.item.domain.po.Item;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.service.IItemService;
import com.hmall.search.service.ISearchService;
import org.springframework.stereotype.Service;

@Service
public class searchServiceImpl  extends ServiceImpl<ItemMapper, Item> implements ISearchService {
}
