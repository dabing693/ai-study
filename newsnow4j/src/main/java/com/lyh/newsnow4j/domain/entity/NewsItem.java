package com.lyh.newsnow4j.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("news_items")
@Data
public class NewsItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private Long createdAt = System.currentTimeMillis();
    private String extraInfo;
    private Integer important;
    private String itemId;
    private String mobileUrl;
    private Long pubDate;
    private String source;
    private String title;
    private String url;
}