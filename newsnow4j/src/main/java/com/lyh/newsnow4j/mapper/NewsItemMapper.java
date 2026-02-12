package com.lyh.newsnow4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyh.newsnow4j.domain.entity.NewsItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NewsItemMapper extends BaseMapper<NewsItem> {
    @Select(value = "SELECT * FROM news_items WHERE source = #{source} ORDER BY pub_date DESC LIMIT #{limit}")
    List<NewsItem> findLatestBySource(@Param("source") String source,
                                      @Param("limit") int limit);
}
