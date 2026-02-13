package com.lyh.base.agent.define;

import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.StreamChatResult;
import com.lyh.base.agent.domain.StreamEvent;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.domain.message.ToolMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
public class ReactiveReActAgent extends ReActAgent {
    public ReactiveReActAgent(ChatModel chatModel, MemoryManager memoryManager, ToolManager toolManager) {
        super(chatModel, memoryManager, toolManager);
    }

    /**
     * 响应式非流式接口
     *
     * @param query
     * @return
     */
    public Mono<ChatResponse> chatMono(String query) {
        return Mono.deferContextual(context -> {
                    RequestContext.copyUserContextFromReactive(context);
                    return Mono.fromCallable(() -> chat(query))
                            .doFinally(signalType -> RequestContext.clear());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 响应式流接口 (基于 Reactor 算子实现的真正响应式循环)
     *
     * @param query
     * @return
     */
    public Flux<StreamEvent> chatFluxReactive(String query) {
        return Flux.<StreamEvent>deferContextual(context -> {
                    RequestContext.copyUserContextFromReactive(context);
                    List<Message> messageList = sense(query);
                    return chatLoopFlux(messageList, 0);
                })
                .doFinally(signalType -> RequestContext.clear())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<StreamEvent> chatLoopFlux(List<Message> messageList, int loopCount) {
        if (loopCount >= getMaxLoopNum()) {
            return Flux.<StreamEvent>empty();
        }

        // 1. 创建当前轮次的流
        Flux<StreamEvent> currentFlux = Flux.<StreamEvent>create(sink -> {
            // 发送助手开始事件
            sink.next(StreamEvent.assistantStart(loopCount));

            // 执行模型流式调用
            StreamChatResult streamResult = chatModel.stream(
                    messageList,
                    toolManager.getTools(),
                    event -> {
                        if (!sink.isCancelled()) {
                            sink.next(event);
                        }
                    });

            AssistantMessage assistantMessage = streamResult.getMessage();
            addAndSave(messageList, assistantMessage);

            if (streamResult.hasToolCalls()) {
                // 处理工具调用
                for (AssistantMessage.ToolCall toolCall : streamResult.getToolCalls()) {
                    ToolMessage toolMessage = toolManager.invoke(toolCall);
                    if (toolMessage != null) {
                        addAndSave(messageList, toolMessage);
                        sink.next(StreamEvent.toolResult(toolMessage));
                    }
                }
                sink.complete();
            } else {
                sink.complete();
            }
        });

        // 2. 递归连接下一轮次的流
        return currentFlux.concatWith(Flux.<StreamEvent>defer(() -> {
            Message lastMsg = messageList.get(messageList.size() - 1);
            if (lastMsg instanceof ToolMessage || (lastMsg instanceof AssistantMessage && ((AssistantMessage) lastMsg).hasToolCalls())) {
                return chatLoopFlux(messageList, loopCount + 1);
            }
            return Flux.<StreamEvent>empty();
        }));
    }

    /**
     * 响应式流接口 (旧的包装方式)
     *
     * @param query
     * @return
     */
    public Flux<StreamEvent> chatFlux(String query) {
        return Flux.<StreamEvent>create(sink -> {
            try {
                chatStream(query, event -> {
                    if (!sink.isCancelled()) {
                        sink.next(event);
                    }
                });
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
