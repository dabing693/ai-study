package com.lyh.base.agent.handler;

import com.lyh.base.agent.model.chat.ChatModel;
import org.springframework.stereotype.Component;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/12 22:53
 */
@Component
public class SummaryModelHandler extends ModelHandler {
    public SummaryModelHandler(ChatModel chatModel) {
        super(chatModel);
    }
}
