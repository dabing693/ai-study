/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
package com.lyh.base.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyh.base.agent.domain.DO.LlmMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LlmMemoryMapper extends BaseMapper<LlmMemory> {

    @Select("SELECT * FROM llm_memory WHERE conversation_id = #{conversationId} ORDER BY timestamp ASC")
    List<LlmMemory> selectByConversationId(@Param("conversationId") String conversationId);
}
