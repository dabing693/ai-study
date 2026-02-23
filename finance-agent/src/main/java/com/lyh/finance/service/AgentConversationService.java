package com.lyh.finance.service;

import com.lyh.finance.domain.entity.AgentConversationMapping;
import com.lyh.finance.mapper.AgentConversationMappingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentConversationService {

    @Autowired
    private AgentConversationMappingMapper mappingMapper;

    public void createMapping(String parentConversationId, String agentName,
                              String agentConversationId, String agentDescription) {
        AgentConversationMapping mapping = new AgentConversationMapping();
        mapping.setParentConversationId(parentConversationId);
        mapping.setAgentName(agentName);
        mapping.setAgentConversationId(agentConversationId);
        mapping.setAgentDescription(agentDescription);
        mapping.setStatus("pending");
        mapping.setCreateTime(LocalDateTime.now());
        mapping.setUpdateTime(LocalDateTime.now());
        mappingMapper.insert(mapping);
    }

    public void updateAgentStatus(String parentConversationId, String agentName,
                                  String status, LocalDateTime startTime, LocalDateTime endTime) {
        AgentConversationMapping mapping = mappingMapper.selectByParentAndAgentName(parentConversationId, agentName);
        if (mapping != null) {
            mapping.setStatus(status);
            if (startTime != null) {
                mapping.setStartTime(startTime);
            }
            if (endTime != null) {
                mapping.setEndTime(endTime);
            }
            mapping.setUpdateTime(LocalDateTime.now());
            mappingMapper.updateById(mapping);
        }
    }

    public List<AgentConversationMapping> getAgentMappings(String parentConversationId) {
        return mappingMapper.selectByParentConversationId(parentConversationId);
    }

    public AgentConversationMapping getAgentMapping(String parentConversationId, String agentName) {
        return mappingMapper.selectByParentAndAgentName(parentConversationId, agentName);
    }

    public String getAgentConversationId(String parentConversationId, String agentName) {
        AgentConversationMapping mapping = mappingMapper.selectByParentAndAgentName(parentConversationId, agentName);
        return mapping != null ? mapping.getAgentConversationId() : null;
    }

    public void deleteMappingsByParentConversationId(String parentConversationId) {
        List<AgentConversationMapping> mappings = mappingMapper.selectByParentConversationId(parentConversationId);
        for (AgentConversationMapping mapping : mappings) {
            mappingMapper.deleteById(mapping.getId());
        }
    }

    public AgentConversationMapping getMappingByAgentConversationId(String agentConversationId) {
        return mappingMapper.selectByAgentConversationId(agentConversationId);
    }
}
