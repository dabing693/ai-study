package com.lyh.finance.tools.akshare;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;


/*
 * @Author:  lengYinHui
 * @Date:  2026/3/9 0:12
 */
@Slf4j
@Service
public class StockIndividualInfoEm extends BaseEmApi {
    private static final Object[] HEAD = new Object[]{"项目", "数值"};

    public StockIndividualInfoEm(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * 获取东方财富-个股-股票信息
     * 对应 Python: ak.stock_individual_info_em
     * 接口地址: https://push2.eastmoney.com/api/qt/stock/get
     *
     * @param symbol  股票代码 (例如: "603777", "000001")
     * @param timeout 超时时间 (秒)，RestTemplate 超时需在 Bean 配置中预先设置
     * @return Object[][]
     * 列顺序: ["item", "value"]
     * 数据行: [["股票代码", "603777"], ["股票简称", "来伊份"], ["总股本", "..."], ...]
     * 如果无数据或映射失败，返回 new Object[0][2]
     */
    public Object[][] stockIndividualInfoEm(String symbol, Double timeout) {
        // 1. 参数准备
        // 市场代码映射: 6 开头为 1 (沪市), 其他为 0 (深市/京市等)
        String marketCode = symbol.startsWith("6") ? "1" : "0";
        String secId = marketCode + "." + symbol;
        // 定义需要获取的字段
        String fields = "f120,f121,f122,f174,f175,f59,f163,f43,f57,f58,f169,f170,f46,f44,f51,f168,f47," +
                "f164,f116,f60,f45,f52,f50,f48,f167,f117,f71,f161,f49,f530,f135,f136,f137,f138," +
                "f139,f141,f142,f144,f145,f147,f148,f140,f143,f146,f149,f55,f62,f162,f92,f173,f104," +
                "f105,f84,f85,f183,f184,f185,f186,f187,f188,f189,f190,f191,f192,f107,f111,f86,f177,f78," +
                "f110,f262,f263,f264,f267,f268,f255,f256,f257,f258,f127,f199,f128,f198,f259,f260,f261," +
                "f171,f277,f278,f279,f288,f152,f250,f251,f252,f253,f254,f269,f270,f271,f272,f273,f274," +
                "f275,f276,f265,f266,f289,f290,f286,f285,f292,f293,f294,f295,f43";

        // 注意：只保留 Python 代码中最终用到的字段
        Map<String, String> codeNameMap = new LinkedHashMap<>();
        codeNameMap.put("f57", "股票代码");
        codeNameMap.put("f58", "股票简称");
        codeNameMap.put("f84", "总股本");
        codeNameMap.put("f85", "流通股");
        codeNameMap.put("f127", "行业");
        codeNameMap.put("f116", "总市值");
        codeNameMap.put("f117", "流通市值");
        codeNameMap.put("f189", "上市时间");
        codeNameMap.put("f43", "最新");

        String url = "https://push2.eastmoney.com/api/qt/stock/get";
        // 2. 构建请求 URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("fltt", "2")
                .queryParam("invt", "2")
                .queryParam("fields", fields)
                .queryParam("secid", secId);

        URI uri = builder.build().toUri();
        HttpHeaders headers = buildHeaders("push2.eastmoney.com");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        // 3. 发送请求
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Request failed for symbol info: " + symbol, e);
        }
        if (response.getBody() == null) {
            return new Object[0][2];
        }
        // 4. 解析 JSON 并转换
        try {
            JSONObject rootNode = JSONObject.parseObject(response.getBody());
            // 检查是否有 data 节点
            JSONObject dataNode = rootNode.getJSONObject("data");
            if (dataNode == null || dataNode.isEmpty()) {
                log.warn("无数据: {}", symbol);
                return new Object[0][2];
            }
            // 收集需要保留的数据行
            List<Object[]> resultList = new ArrayList<>();
            // 遍历映射表，从 dataNode 中提取对应的值
            for (Map.Entry<String, String> entry : codeNameMap.entrySet()) {
                String fieldKey = entry.getKey();
                String fieldNameCn = entry.getValue();
                if (dataNode.containsKey(fieldKey)) {
                    Object value = dataNode.get(fieldKey);
                    String strValue = value == null ? null : value.toString();
                    // 处理 null 或特定无效值
                    if (strValue != null && !"null".equals(strValue) && !"".equals(strValue)) {
                        resultList.add(new Object[]{fieldNameCn, value.toString()});
                    }
                }
            }
            if (resultList.isEmpty()) {
                return new Object[0][2];
            }
            Object[][] result = new Object[resultList.size() + 1][2];
            result[0] = HEAD;
            for (int i = 0; i < resultList.size(); i++) {
                result[i + 1] = resultList.get(i);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("请求接口失败: " + uri.toString(), e);
        }
    }
}
