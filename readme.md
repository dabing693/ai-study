mvn archetype:generate -DgroupId=com.lyh -DartifactId=ai-agents -Dversion=0.0.1-SNAPSHOT -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

1、不使用ToolCallAdvisor时，ZhiPuAiChatModel的call方法里面有判断是否需要调用工具的逻辑：
return toolExecutionResult.returnDirect() ? ChatResponse.builder().from(response).generations(ToolExecutionResult.buildGenerations(toolExecutionResult)).build() : this.call(new Prompt(toolExecutionResult.conversationHistory(), requestPrompt.getOptions()));
2、使用ToolCallAdvisor时，ToolCallAdvisor的adviseCall方法里面有判断是否需要调用的逻辑：
boolean isToolCall = false;
do {
    ...
    isToolCall = chatClientResponse.chatResponse() != null && chatClientResponse.chatResponse().hasToolCalls();
    ...
} while(isToolCall);

3、底层使用spring-ai-starter-mcp-server-webflux、spring-boot-starter-webflux时，
"http://localhost:9080/chat/generateFux?query=资金账号是18967543" 会报错：
org.springframework.web.client.ResourceAccessException: I/O error on POST request for "https://open.bigmodel.cn/api/paas/v4/chat/completions ": block()/blockFirst()/blockLast() are blocking, which is not supported in thread reactor-http-nio-5
因为：Spring WebFlux + WebClient 的阻塞调用错误。错误信息表明你在反应式（Reactive）线程中使用了阻塞操作。