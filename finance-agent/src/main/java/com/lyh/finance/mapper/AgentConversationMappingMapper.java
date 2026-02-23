package com.lyh.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyh.finance.domain.entity.AgentConversationMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentConversationMappingMapper extends BaseMapper<AgentConversationMapping> {

    @Select("SELECT * FROM agent_conversation_mapping WHERE parent_conversation_id = #{parentConversationId} ORDER BY create_time ASC")
    List<AgentConversationMapping> selectByParentConversationId(@Param("parentConversationId") String parentConversationId);

    @Select("SELECT * FROM agent_conversation_mapping WHERE parent_conversation_id = #{parentConversationId} AND agent_name = #{agentName}")
    AgentConversationMapping selectByParentAndAgentName(@Param("parentConversationId") String parentConversationId, @Param("agentName") String agentName);

    @Select("SELECT * FROM agent_conversation_mapping WHERE agent_conversation_id = #{agentConversationId}")
    AgentConversationMapping selectByAgentConversationId(@Param("agentConversationId") String agentConversationId);
}
