package com.lyh.finance.tools.akshare;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/8 23:53
 */
public class BaseEmApi {
    protected static final int maxNum = 20;
    protected final RestTemplate restTemplate;

    public BaseEmApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    protected HttpHeaders buildHeaders(String host) {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        //开启时会告诉服务器：我支持压缩，发gzip压缩后的数据给我，因此RestTemplate需要处理解压缩
        //headers.add("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        headers.add("Cache-Control", "max-age=0");
        headers.add("Connection", "keep-alive");

        // Cookie 非常关键，必须完整复制
        headers.add("Cookie", "qgqp_b_id=5ffab92129f06e2f7d00e9f2d3f7c904; st_nvi=vaPpvpH9qPe9BlTDeirWWbae9; nid18=0389334c3e686411ec7ffa6e0d96074f; nid18_create_time=1766817058951; gviem=bzyfzpdllKyisxnfgsWZI605e; gviem_create_time=1766817058951");

        headers.add("Host", host);
        headers.add("Sec-Fetch-Dest", "document");
        headers.add("Sec-Fetch-Mode", "navigate");
        headers.add("Sec-Fetch-Site", "none");
        headers.add("Sec-Fetch-User", "?1");
        headers.add("Upgrade-Insecure-Requests", "1");
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0");

        // sec-ch-ua 包含分号和引号，直接作为字符串添加即可
        headers.add("sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Microsoft Edge\";v=\"145\", \"Chromium\";v=\"145\"");
        headers.add("sec-ch-ua-mobile", "?0");
        headers.add("sec-ch-ua-platform", "\"Windows\"");
        return headers;
    }
}
