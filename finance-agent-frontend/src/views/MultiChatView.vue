<template>
  <section class="content">
    <div v-if="Object.keys(agentData).length === 0" class="welcome">
      <h1 class="welcome__title">Multi Agent 会诊</h1>
      <p class="welcome__subtitle">
        多个专业 Agent 协同分析，为您提供全面的投资建议
      </p>
    </div>

    <div v-else class="chat">
      <div class="agent-selector">
        <select v-model="selectedAgent" class="agent-selector__dropdown">
          <option v-for="agent in agentList" :key="agent.name" :value="agent.name">
            {{ agent.name }}
            <template v-if="getAgentStatus(agent.name)">
              ({{ getStatusLabel(getAgentStatus(agent.name)) }})
            </template>
          </option>
        </select>
        <div class="agent-selector__status">
          <span
            v-for="agent in agentList"
            :key="agent.name"
            class="status-dot"
            :class="getAgentStatus(agent.name)"
            :title="agent.name"
          ></span>
        </div>
      </div>

      <div class="chat__list" ref="chatList">
        <template v-if="currentAgentMessages.length > 0">
          <div
            v-for="(msg, idx) in currentAgentMessages"
            :key="idx"
            class="chat__item"
            :class="msg.role"
          >
            <div class="chat__bubble">
              <div class="chat__role">{{ msg.roleLabel }}</div>
              <div
                v-if="msg.role === 'assistant' && msg.streaming && !getAssistantDisplayContent(msg)"
                class="chat__content chat__typing"
              >
                <span></span><span></span><span></span>
              </div>
              <Markdown
                v-else-if="getAssistantDisplayContent(msg)"
                class="chat__content"
                :source="getAssistantDisplayContent(msg)"
                :breaks="false"
              />
              <div class="chat__actions">
                <button
                  class="chat__action-btn"
                  type="button"
                  title="复制"
                  @click="copyMessage(msg.role === 'assistant' ? getAssistantDisplayContent(msg) : msg.content)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="chat__empty">
          <p>该 Agent 暂无消息</p>
        </div>
        <div ref="scrollAnchor"></div>
      </div>
    </div>

    <div class="composer">
      <div class="composer__input">
        <textarea
          ref="textareaRef"
          v-model="input"
          class="composer__textarea"
          placeholder="输入问题，多个 Agent 将协同分析"
          rows="3"
          @keydown.enter.exact.prevent="sendMessage"
          @keydown.enter.shift.exact.stop
        ></textarea>
        <div class="composer__buttons">
          <div class="composer__buttons-left">
            <span class="composer__plus">＋</span>
            <div class="agent-toggle">
              <router-link to="/react-chat" class="agent-toggle__btn">
                React
              </router-link>
              <router-link to="/multi-chat" class="agent-toggle__btn" :class="{ active: true }">
                Multi
              </router-link>
            </div>
          </div>
          <div class="composer__right-buttons">
            <button
              class="send-btn"
              type="button"
              :disabled="loading || !input.trim()"
              @click="sendMessage"
              title="发送"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 12 21 4l-4 16-5-6-9-2z" />
              </svg>
            </button>
          </div>
        </div>
      </div>
      <div class="composer__hint" v-if="error">{{ error }}</div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Markdown from "vue3-markdown-it";
import { useAuthStore } from "../stores/auth.js";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const input = ref("");
const loading = ref(false);
const error = ref("");
const chatList = ref(null);
const scrollAnchor = ref(null);
const parentConversationId = ref("");
const textareaRef = ref(null);
const selectedAgent = ref("总结报告Agent");
let sseController = null;
let resizeObserver = null;
const streamingActive = ref(false);

const agentList = [
  { name: "技术分析Agent", description: "负责分析股票的技术面：K线、技术指标、趋势等" },
  { name: "基本面分析Agent", description: "负责分析股票的基本面：财报、估值、成长性等" },
  { name: "市场情绪Agent", description: "负责分析市场情绪：新闻、舆情、资金流向等" },
  { name: "风险评估Agent", description: "负责基于前面的分析结果进行风险评估" },
  { name: "总结报告Agent", description: "负责整合所有分析结果，生成最终报告" },
];

const agentData = ref({});

const currentAgentMessages = computed(() => {
  const agent = agentData.value[selectedAgent.value];
  return agent?.messages || [];
});

const getAgentStatus = (agentName) => {
  return agentData.value[agentName]?.status || "pending";
};

const getStatusLabel = (status) => {
  const labels = {
    pending: "等待中",
    running: "执行中",
    success: "完成",
    error: "失败",
  };
  return labels[status] || status;
};

const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content);
  } catch (err) {
    console.error("复制失败:", err);
  }
};

const adjustTextareaHeight = () => {
  const textarea = textareaRef.value;
  if (textarea) {
    if (!input.value || input.value.trim() === "") {
      textarea.style.height = "";
    } else {
      textarea.style.height = "auto";
      textarea.style.height = `${textarea.scrollHeight}px`;
    }
  }
};

watch(input, () => {
  nextTick(() => {
    adjustTextareaHeight();
  });
});

const getAssistantDisplayContent = (msg) => {
  if (!msg || msg.role !== "assistant") return msg?.content || "";

  const reasoningContent = msg.reasoningContent ? `思考内容：\n${msg.reasoningContent}` : "";
  const content = msg.content || "";
  const toolCalls = msg.toolCalls || [];

  let fullContent = reasoningContent;
  if (content) {
    fullContent = reasoningContent ? `${reasoningContent}\n\n${content}` : content;
  }

  if (toolCalls.length > 0) {
    const toolCallsText = toolCalls
      .map((tc) => {
        const name = tc.function?.name || "Tool";
        const args = tc.function?.arguments || "{}";
        return `工具名称: ${name} \n\n工具参数:\n\`\`\`json\n${args}\n\`\`\``;
      })
      .join("\n\n");

    fullContent = fullContent ? `${fullContent}\n\n${toolCallsText}` : toolCallsText;
  }

  return fullContent;
};

const scrollToBottom = () => {
  const list = chatList.value;
  if (!list) return;

  const composerEl = document.querySelector(".composer");
  const composerHeight = composerEl ? composerEl.offsetHeight + 32 : 120;

  requestAnimationFrame(() => {
    const lastMessage = list.querySelector(".chat__item:last-of-type");
    if (lastMessage) {
      const messageBottom = lastMessage.offsetTop + lastMessage.offsetHeight;
      let targetScrollTop = messageBottom + composerHeight - list.clientHeight;
      targetScrollTop = Math.max(0, targetScrollTop);
      list.scrollTop = targetScrollTop;
    } else {
      const maxScrollTop = Math.max(0, list.scrollHeight - list.clientHeight);
      list.scrollTop = maxScrollTop;
    }
  });
};

onMounted(async () => {
  console.log("MultiChatView onMounted, route.params:", route.params);

  const convId = route.params.id;
  console.log("convId from route.params.id:", convId);
  if (convId) {
    await loadHistoryMessages(convId);
  }

  await nextTick();
  const list = chatList.value;
  if (!list || typeof ResizeObserver === "undefined") return;
  resizeObserver = new ResizeObserver(() => {
    if (streamingActive.value) {
      scrollToBottom();
    }
  });
  resizeObserver.observe(list);
});

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
});

const buildUuid = () => crypto.randomUUID().replace(/-/g, "");

const parseSseStream = async (response, onEvent) => {
  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error("Empty response body.");
  }
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  let currentEvent = "message";
  let dataLines = [];

  const dispatch = async () => {
    if (!dataLines.length) return;
    const data = dataLines.join("\n");
    await onEvent(currentEvent, data);
    currentEvent = "message";
    dataLines = [];
  };

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let lineBreakIndex = buffer.indexOf("\n");
    while (lineBreakIndex >= 0) {
      let line = buffer.slice(0, lineBreakIndex);
      buffer = buffer.slice(lineBreakIndex + 1);
      if (line.endsWith("\r")) {
        line = line.slice(0, -1);
      }
      if (!line) {
        await dispatch();
        lineBreakIndex = buffer.indexOf("\n");
        continue;
      }
      if (line.startsWith(":")) {
        lineBreakIndex = buffer.indexOf("\n");
        continue;
      }
      if (line.startsWith("event:")) {
        currentEvent = line.slice(6).trim() || "message";
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice(5).trimStart());
      }
      lineBreakIndex = buffer.indexOf("\n");
    }
  }

  if (buffer.trim()) {
    const line = buffer.replace(/\r$/, "");
    if (line.startsWith("event:")) {
      currentEvent = line.slice(6).trim() || "message";
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trimStart());
    }
  }
  await dispatch();
};

const initAgentData = () => {
  agentList.forEach((agent) => {
    agentData.value[agent.name] = {
      conversationId: "",
      messages: [],
      status: "pending",
      streaming: false,
    };
  });
};

const sendMessage = async () => {
  const text = input.value.trim();
  if (!text || loading.value) return;

  error.value = "";
  if (sseController) {
    sseController.abort();
    sseController = null;
  }

  initAgentData();

  input.value = "";
  loading.value = true;

  let isNewSession = false;
  if (!parentConversationId.value) {
    parentConversationId.value = buildUuid();
    isNewSession = true;
    router.replace({ name: "multi-conversation", params: { id: parentConversationId.value } });
  }

  const url = new URL("/multi-agent/consultation/stream", window.location.origin);
  url.searchParams.set("query", text);
  if (parentConversationId.value) {
    url.searchParams.set("conversationId", parentConversationId.value);
  }

  sseController = new AbortController();
  try {
    const headers = {
      isNew: isNewSession ? "true" : "false",
    };
    if (authStore.state.isLoggedIn && authStore.state.token) {
      headers["Authorization"] = `Bearer ${authStore.state.token}`;
    }
    const response = await fetch(url.toString(), {
      headers,
      signal: sseController.signal,
    });

    if (!response.ok) {
      throw new Error(`Request failed with ${response.status}`);
    }

    streamingActive.value = true;

    await parseSseStream(response, async (eventName, data) => {
      let payload;
      try {
        payload = data ? JSON.parse(data) : {};
      } catch (e) {
        payload = {};
      }

      switch (eventName) {
        case "session":
          if (!parentConversationId.value && data) {
            parentConversationId.value = data;
          }
          break;

        case "start":
          break;

        case "agent_start": {
          const agentName = payload.agentName;
          if (!agentData.value[agentName]) {
            agentData.value[agentName] = {
              conversationId: "",
              messages: [],
              status: "pending",
              streaming: false,
            };
          }
          agentData.value[agentName].conversationId = payload.agentConversationId;
          agentData.value[agentName].status = "running";
          agentData.value[agentName].streaming = true;
          agentData.value[agentName].messages = [
            { role: "user", roleLabel: "你", content: text },
            {
              role: "assistant",
              roleLabel: agentName,
              content: "",
              reasoningContent: "",
              toolCalls: [],
              streaming: true,
            },
          ];
          break;
        }

        case "agent_delta": {
          const agentName = payload.agentName;
          const agent = agentData.value[agentName];
          if (agent?.messages.length > 0) {
            const lastMsg = agent.messages[agent.messages.length - 1];
            if (lastMsg.role === "assistant") {
              lastMsg.content += payload.content || "";
            }
          }
          break;
        }

        case "agent_reasoning": {
          const agentName = payload.agentName;
          const agent = agentData.value[agentName];
          if (agent?.messages.length > 0) {
            const lastMsg = agent.messages[agent.messages.length - 1];
            if (lastMsg.role === "assistant") {
              lastMsg.reasoningContent = payload.reasoningContent || "";
            }
          }
          break;
        }

        case "agent_tool_call": {
          const agentName = payload.agentName;
          const agent = agentData.value[agentName];
          if (agent?.messages.length > 0) {
            const lastMsg = agent.messages[agent.messages.length - 1];
            if (lastMsg.role === "assistant") {
              try {
                const toolCalls = JSON.parse(payload.toolCalls);
                lastMsg.toolCalls.push(...toolCalls);
              } catch (e) {}
            }
          }
          break;
        }

        case "agent_tool_result": {
          const agentName = payload.agentName;
          const agent = agentData.value[agentName];
          if (agent) {
            agent.messages.push({
              role: "tool",
              roleLabel: "Tool Result",
              content: payload.toolResult || payload.content || "",
            });
          }
          break;
        }

        case "agent_done": {
          const agentName = payload.agentName;
          const agent = agentData.value[agentName];
          if (agent) {
            agent.status = payload.status || "success";
            agent.streaming = false;
            if (agent.messages.length > 0) {
              const lastMsg = agent.messages[agent.messages.length - 1];
              if (lastMsg.role === "assistant") {
                lastMsg.streaming = false;
              }
            }
          }
          break;
        }

        case "agent_error": {
          const agentName = payload.agentName;
          const agent = agentData.value[agentName];
          if (agent) {
            agent.status = "error";
            agent.streaming = false;
            if (agent.messages.length > 0) {
              const lastMsg = agent.messages[agent.messages.length - 1];
              if (lastMsg.role === "assistant") {
                lastMsg.streaming = false;
                lastMsg.content = payload.content || "执行失败";
              }
            }
          }
          break;
        }

        case "content":
          break;

        case "done":
          loading.value = false;
          streamingActive.value = false;
          if (sseController) {
            sseController.abort();
            sseController = null;
          }
          break;

        case "error":
          error.value = data || "Request failed. Please check the backend service.";
          loading.value = false;
          streamingActive.value = false;
          if (sseController) {
            sseController.abort();
            sseController = null;
          }
          break;
      }

      await nextTick();
      scrollToBottom();
    });
  } catch (err) {
    if (err && err.name === "AbortError") {
      return;
    }
    error.value = "Request failed. Please check the backend service.";
    loading.value = false;
    streamingActive.value = false;
    if (sseController) {
      sseController.abort();
      sseController = null;
    }
  }
};

const newConversation = () => {
  parentConversationId.value = "";
  agentData.value = {};
  router.push({ name: "multi-chat" });
};

const loadConversation = async (conv) => {
  parentConversationId.value = conv.conversationId;
  router.push({ name: "multi-conversation", params: { id: conv.conversationId } });
};

const loadHistoryMessages = async (convId) => {
  if (!convId) return;

  console.log("loadHistoryMessages called with convId:", convId);
  parentConversationId.value = convId;

  try {
    const response = await authStore.fetchWithAuth(
      `/multi-agent/agents/${convId}`
    );
    console.log("Agent mappings response:", response);
    if (response && response.ok) {
      const mappings = await response.json();
      console.log("Agent mappings:", mappings);

      for (const mapping of mappings) {
        const agentName = mapping.agentName;
        const agentConversationId = mapping.agentConversationId;

        agentData.value[agentName] = {
          conversationId: agentConversationId,
          messages: [],
          status: mapping.status || "success",
          streaming: false,
        };

        const msgResponse = await authStore.fetchWithAuth(
          `/api/conversation/${agentConversationId}/messages`
        );
        if (msgResponse && msgResponse.ok) {
          const messages = await msgResponse.json();
          if (messages && messages.length > 0) {
            agentData.value[agentName].messages = parseHistoryMessages(messages, agentName);
          }
        }
      }
    }
  } catch (err) {
    console.error("加载历史消息失败:", err);
  }
};

const parseHistoryMessages = (historyMessages, agentName) => {
  const parsedMessages = [];

  historyMessages
    .filter((msg) => msg.type === "user" || msg.type === "assistant" || msg.type === "tool")
    .forEach((msg) => {
      if (msg.type === "user") {
        parsedMessages.push({
          role: "user",
          roleLabel: "你",
          content: msg.content,
          reasoningContent: "",
          toolCalls: [],
        });
      } else if (msg.type === "assistant") {
        const assistantMsg = {
          role: "assistant",
          roleLabel: agentName,
          content: "",
          reasoningContent: "",
          toolCalls: [],
        };
        try {
          const jsonData = JSON.parse(msg.content);
          if (jsonData.content) {
            assistantMsg.content = jsonData.content;
          }
          if (jsonData.reasoning_content) {
            assistantMsg.reasoningContent = jsonData.reasoning_content;
          }
          if (jsonData.tool_calls) {
            assistantMsg.toolCalls = JSON.parse(jsonData.tool_calls);
          }
        } catch (e) {
          assistantMsg.content = msg.content;
        }
        parsedMessages.push(assistantMsg);
      } else if (msg.type === "tool") {
        parsedMessages.push({
          role: "tool",
          roleLabel: "Tool Result",
          content: msg.content,
          reasoningContent: "",
          toolCalls: [],
        });
      }
    });

  return parsedMessages;
};

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadHistoryMessages(newId);
    }
  }
);

defineExpose({
  newConversation,
  loadConversation,
});
</script>

<style scoped>
.agent-selector {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #f7f7f8;
  border-bottom: 1px solid #e5e5e5;
}

.agent-selector__dropdown {
  flex: 1;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  cursor: pointer;
}

.agent-selector__dropdown:focus {
  outline: none;
  border-color: #10a37f;
}

.agent-selector__status {
  display: flex;
  gap: 6px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
}

.status-dot.pending {
  background: #d1d5db;
}

.status-dot.running {
  background: #fbbf24;
  animation: pulse 1s infinite;
}

.status-dot.success {
  background: #10b981;
}

.status-dot.error {
  background: #ef4444;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.chat__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #9ca3af;
}
</style>
