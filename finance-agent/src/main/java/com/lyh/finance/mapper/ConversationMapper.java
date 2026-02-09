package com.lyh.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyh.finance.domain.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversation WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<Conversation> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM conversation WHERE conversation_id = #{conversationId} AND user_id = #{userId}")
    Conversation selectByConversationIdAndUserId(@Param("conversationId") String conversationId, @Param("userId") Long userId);
}
