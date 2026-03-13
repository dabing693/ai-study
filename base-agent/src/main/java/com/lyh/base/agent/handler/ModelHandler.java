package com.lyh.base.agent.handler;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.domain.message.UserMessage;
import com.lyh.base.agent.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class ModelHandler {
    protected final ChatModel chatModel;

    public String handle(String... args) {
        UserMessage systemMessage = systemMessage(args);
        return chatModel.call(systemMessage).getReply();
    }

    public UserMessage systemMessage(String... args) {
        try {
            String superClzName = this.getClass().getSuperclass().getSimpleName();
            String promptFile = PropertyNamingStrategies.KEBAB_CASE.nameForField(null, null,
                    this.getClass().getSimpleName().replace(superClzName, "")) + ".txt";
            Resource promptResource = new ClassPathResource("prompt/" + promptFile);
            InputStreamReader reader = new InputStreamReader(
                    promptResource.getInputStream(), StandardCharsets.UTF_8);
            String template = FileCopyUtils.copyToString(reader);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    template = template.replace(String.format("{arg%s}", i + 1), args[i]);
                }
            }
            return new UserMessage(template);
        } catch (Exception e) {
            log.error("获取系统提示词失败");
            throw new RuntimeException("获取系统提示词失败");
        }
    }
}
