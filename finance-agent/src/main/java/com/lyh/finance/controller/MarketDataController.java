package com.lyh.finance.controller;

import com.lyh.finance.tools.AkShareTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    @Autowired
    private AkShareTool akShareTool;

    @GetMapping("/overview")
    public Map<String, Object> getMarketOverview() {
        Map<String, Object> result = new HashMap<>();
        result.put("hotStocks", akShareTool.getHotStocksRaw());
        result.put("hotIndustries", akShareTool.getHotIndustriesRaw());
        result.put("lhbData", akShareTool.getLhbDataRaw());
        return result;
    }
}
