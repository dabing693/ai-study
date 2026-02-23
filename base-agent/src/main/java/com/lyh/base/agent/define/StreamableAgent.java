package com.lyh.base.agent.define;

import com.lyh.base.agent.domain.StreamEvent;

import java.util.function.Consumer;

public interface StreamableAgent {
    void chatStream(String query, Consumer<StreamEvent> eventConsumer);
}
