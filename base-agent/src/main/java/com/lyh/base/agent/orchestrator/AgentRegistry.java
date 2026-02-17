package com.lyh.base.agent.orchestrator;

import com.lyh.base.agent.define.BaseAgent;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 注册表
 * 管理所有可用的 Agent
 */
@Slf4j
public class AgentRegistry {

    private final Map<String, BaseAgent> agents = new ConcurrentHashMap<>();
    private final Map<String, String> agentDescriptions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> agentTags = new ConcurrentHashMap<>();

    /**
     * 注册 Agent
     */
    public void register(String name, String description, BaseAgent agent, String... tags) {
        agents.put(name, agent);
        agentDescriptions.put(name, description);
        agentTags.put(name, Arrays.asList(tags));
        log.info("Agent 注册成功: name={}, description={}", name, description);
    }

    /**
     * 获取 Agent
     */
    public BaseAgent getAgent(String name) {
        return agents.get(name);
    }

    /**
     * 获取 Agent 描述
     */
    public String getAgentDescription(String name) {
        return agentDescriptions.get(name);
    }

    /**
     * 获取所有 Agent 名称
     */
    public Set<String> getAllAgentNames() {
        return agents.keySet();
    }

    /**
     * 根据标签查找 Agent
     */
    public List<String> findAgentsByTag(String tag) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : agentTags.entrySet()) {
            if (entry.getValue().contains(tag)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 检查 Agent 是否存在
     */
    public boolean contains(String name) {
        return agents.containsKey(name);
    }
}
