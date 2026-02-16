<template>
  <section class="content">
    <div v-if="messages.length === 0" class="welcome">
      <h1 class="welcome__title">今天有什么计划？</h1>
      <p class="welcome__subtitle">
        让 Finance Agent 帮你梳理、拆解、回答问题
      </p>
    </div>

    <div v-else class="chat">
      <div class="chat__list" ref="chatList">
        <div
          v-for="(msg, idx) in messages"
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
              <button
                class="chat__action-btn"
                type="button"
                title="点赞"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
              </button>
              <button
                class="chat__action-btn"
                type="button"
                title="点踩"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M15 3H6c-.83 0-1.54.5-1.84 1.22l-3.02 7.05c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.31l-.95 4.57-.03.32c0 .41.17.79.44 1.06L9.83 23l6.59-6.59c.36-.36.58-.86.58-1.41V5c0-1.1-.9-2-2-2zm4 0v12h4V3h-4z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
        <div ref="scrollAnchor"></div>
      </div>
    </div>

    <div class="composer">
      <div class="composer__input">
        <span class="composer__plus">＋</span>
        <textarea
          ref="textareaRef"
          v-model="input"
          class="composer__textarea"
          placeholder="有问题，尽管问"
          rows="1"
          @keydown.enter.exact.prevent="sendMessage"
          @keydown.enter.shift.exact.stop
        ></textarea>
        <button class="icon-btn" type="button" title="语音">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3zm-5 9h2a5 5 0 0 0 10 0h2a7 7 0 0 1-6 6.9V21h-2v-2.1A7 7 0 0 1 7 12z"
            />
          </svg>
        </button>
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
      <div class="composer__hint" v-if="error">{{ error }}</div>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Markdown from "vue3-markdown-it";
import { useAuthStore } from "../stores/auth.js";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const input = ref("");
const messages = ref([]);
const loading = ref(false);
const error = ref("");
const chatList = ref(null);
const scrollAnchor = ref(null);
const conversationId = ref("");
const textareaRef = ref(null);
let sseController = null;
let pendingScroll = false;
let flushTimer = null;
const FLUSH_INTERVAL = 50;
let autoScrollTimer = null;
let resizeObserver = null;
const streamingActive = ref(false);

const emit = defineEmits(['analyzeNews']);

const loadHistoryMessages = async (forceReload = false) => {
  const convId = route.params.id;
  if (!convId) return;
  
  if (!authStore.state.isLoggedIn) {
    return;
  }
  
  if (!forceReload && messages.value.length > 0) return;
  
  conversationId.value = convId;
  
  try {
    const response = await authStore.fetchWithAuth(
      `/api/conversation/${convId}/messages`
    );
    if (response && response.ok) {
      const historyMessages = await response.json();
      if (historyMessages && historyMessages.length > 0) {
        messages.value = parseHistoryMessages(historyMessages);
      }
    }
  } catch (err) {
    console.error('加载历史对话失败:', err);
  }
};

watch(() => authStore.state.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    loadHistoryMessages(true);
  }
});

watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    messages.value = [];
    loadHistoryMessages(true);
  }
});

const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content);
    console.log('内容已复制到剪贴板');
  } catch (err) {
    console.error('复制失败:', err);
  }
};

const adjustTextareaHeight = () => {
  const textarea = textareaRef.value;
  if (textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = `${textarea.scrollHeight}px`;
  }
};

watch(input, () => {
  nextTick(() => {
    adjustTextareaHeight();
  });
});

const getAssistantDisplayContent = (msg) => {
  if (!msg || msg.role !== 'assistant') return msg?.content || '';

  const reasoningContent = msg.reasoningContent ? `思考内容：\n${msg.reasoningContent}` : '';
  const content = msg.content || '';
  const toolCalls = msg.toolCalls || [];

  let fullContent = reasoningContent;
  if (content) {
    fullContent = reasoningContent ? `${reasoningContent}\n\n${content}` : content;
  }

  if (toolCalls.length > 0) {
    const toolCallsText = toolCalls.map(tc => {
      const name = tc.function?.name || 'Tool';
      const args = tc.function?.arguments || '{}';
      return `工具名称: ${name} \n\n工具参数:\n\`\`\`json\n${args}\n\`\`\``;
    }).join('\n\n');

    fullContent = fullContent ? `${fullContent}\n\n${toolCallsText}` : toolCallsText;
  }

  return fullContent;
};

const parseHistoryMessages = (historyMessages) => {
  const parsedMessages = [];

  historyMessages
    .filter(msg => msg.type === 'user' || msg.type === 'assistant' || msg.type === 'tool')
    .forEach(msg => {
      if (msg.type === 'user') {
        parsedMessages.push({
          role: 'user',
          roleLabel: '你',
          content: msg.content,
          reasoningContent: '',
          toolCalls: []
        });
      } else if (msg.type === 'assistant') {
        const assistantMsg = {
          role: 'assistant',
          roleLabel: '助手',
          reasoningContent: '',
          toolCalls: []
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
      } else if (msg.type === 'tool') {
        parsedMessages.push({
          role: 'tool',
          roleLabel: 'Tool Result',
          content: msg.content,
          reasoningContent: '',
          toolCalls: []
        });
      }
    });

  return parsedMessages;
};

const scrollToBottom = () => {
  const list = chatList.value;
  if (!list) return;

  const composerEl = document.querySelector('.composer');
  const composerHeight = composerEl ? composerEl.offsetHeight + 32 : 120;

  requestAnimationFrame(() => {
    const lastMessage = list.querySelector('.chat__item:last-of-type');
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

const scheduleScroll = () => {
  if (pendingScroll) return;
  pendingScroll = true;
  requestAnimationFrame(() => {
    scrollToBottom();
    pendingScroll = false;
  });
};

const startAutoScroll = () => {
  if (autoScrollTimer) return;
  autoScrollTimer = setInterval(() => {
    scrollToBottom();
  }, 50);
};

const stopAutoScroll = () => {
  if (!autoScrollTimer) return;
  clearInterval(autoScrollTimer);
  autoScrollTimer = null;
};

onMounted(async () => {
  await nextTick();
  const list = chatList.value;
  if (!list || typeof ResizeObserver === "undefined") return;
  resizeObserver = new ResizeObserver((entries) => {
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

const scheduleFlush = (message) => {
  if (!message || !message.streaming) return;
  if (flushTimer) return;
  flushTimer = setTimeout(async () => {
    flushTimer = null;
    await nextTick();
    scheduleScroll();
  }, FLUSH_INTERVAL);
};

const buildUuid = () => crypto.randomUUID().replace(/-/g, "");

const finalizeStreamingState = async (assistantMessage) => {
  assistantMessage.streaming = false;
  loading.value = false;
  streamingActive.value = false;
  stopAutoScroll();
  await nextTick();
  scrollToBottom();
};

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

const sendMessage = async () => {
  const text = input.value.trim();
  if (!text || loading.value) return;

  error.value = "";
  if (sseController) {
    sseController.abort();
    sseController = null;
  }
  messages.value.push({
    role: "user",
    roleLabel: "你",
    content: text,
  });
  input.value = "";
  loading.value = true;
  await nextTick();
  scrollToBottom();

  const assistantMessage = reactive({
    role: "assistant",
    roleLabel: "助手",
    content: "",
    reasoningContent: "",
    toolCalls: [],
    streaming: true,
  });
  messages.value.push(assistantMessage);
  await nextTick();
  scrollToBottom();
  streamingActive.value = true;
  startAutoScroll();

  let currentAssistantMessage = assistantMessage;

  const createNewAssistantMessage = (index) => {
    if (currentAssistantMessage) {
      currentAssistantMessage.streaming = false;
    }
    const newMsg = reactive({
      role: "assistant",
      roleLabel: "助手",
      content: "",
      reasoningContent: "",
      toolCalls: [],
      streaming: true,
    });
    messages.value.push(newMsg);
    currentAssistantMessage = newMsg;
    nextTick(() => scrollToBottom());
  };

  let isNewSession = false;
  if (!conversationId.value) {
    conversationId.value = buildUuid();
    isNewSession = true;
    router.replace({ name: 'conversation', params: { id: conversationId.value } });
  }

  const url = new URL("/react/chat/stream", window.location.origin);
  url.searchParams.set("query", text);
  if (conversationId.value) {
    url.searchParams.set("conversationId", conversationId.value);
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

    await nextTick();
    scrollToBottom();

    await parseSseStream(response, async (eventName, data) => {
      if (eventName === "message"){
        try {
          const payload = JSON.parse(data);
          eventName = payload.type;
        } catch (err) {
        }
      }
      if (eventName === "session") {
        try {
          const payload = JSON.parse(data);
          if (payload.conversationId && !conversationId.value) {
            conversationId.value = payload.conversationId;
          }
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "assistant_start") {
        try {
          const payload = JSON.parse(data);
          const index = payload.assistantIndex || 0;
          if (index > 0) {
            createNewAssistantMessage(index);
          }
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "delta") {
        try {
          const payload = JSON.parse(data);
          if (payload.content && currentAssistantMessage) {
            currentAssistantMessage.content += payload.content;
            scheduleFlush(currentAssistantMessage);
          }
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "reasoning_delta") {
        try {
          const payload = JSON.parse(data);
          if (payload.reasoningContent && currentAssistantMessage) {
            currentAssistantMessage.reasoningContent = payload.reasoningContent;
            await nextTick();
            scrollToBottom();
          }
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "tool_call") {
        try {
          const payload = JSON.parse(data);
          if (currentAssistantMessage) {
            const cur_tool_calls = JSON.parse(payload.toolCalls)
            currentAssistantMessage.toolCalls.push(...cur_tool_calls);
            await nextTick();
            scrollToBottom();
          }
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "tool_result") {
        try {
          const payload = JSON.parse(data);
          messages.value.push({
            role: "tool",
            roleLabel: "Tool Result",
            content: payload.content || "",
          });
          await nextTick();
          scrollToBottom();
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "done") {
        await finalizeStreamingState(currentAssistantMessage);
        if (sseController) {
          sseController.abort();
          sseController = null;
        }
        return;
      }
      if (eventName === "error") {
        try {
          const payload = JSON.parse(data);
          error.value = payload.content || "Request failed. Please check the backend service.";
        } catch (err) {
          error.value = "Request failed. Please check the backend service.";
        }
        await finalizeStreamingState(currentAssistantMessage);
        if (sseController) {
          sseController.abort();
          sseController = null;
        }
      }
    });
  } catch (err) {
    if (err && err.name === "AbortError") {
      return;
    }
    error.value = "Request failed. Please check the backend service.";
    await finalizeStreamingState(currentAssistantMessage);
    if (sseController) {
      sseController.abort();
      sseController = null;
    }
  }
};

const newConversation = () => {
  conversationId.value = '';
  messages.value = [];
  router.push({ name: 'chat' });
};

const loadConversation = async (conv) => {
  conversationId.value = conv.conversationId;
  messages.value = [];
  router.push({ name: 'conversation', params: { id: conv.conversationId } });

  if (authStore.state.isLoggedIn) {
    try {
      const response = await authStore.fetchWithAuth(
        `/api/conversation/${conv.conversationId}/messages`
      );
      if (response && response.ok) {
        const historyMessages = await response.json();
        messages.value = parseHistoryMessages(historyMessages);
      }
    } catch (err) {
      console.error('加载历史消息失败:', err);
    }
  }
};

const setAnalyzeContent = (content) => {
  input.value = content;
  nextTick(() => {
    const textarea = document.querySelector('.composer__textarea');
    if (textarea) {
      textarea.focus();
    }
  });
};

onMounted(async () => {
  const convId = route.params.id;
  if (convId) {
    conversationId.value = convId;
    await loadHistoryMessages();
  }
});

watch(
  () => messages.value.length,
  async () => {
    await nextTick();
    scrollToBottom();
  }
);

defineExpose({
  newConversation,
  loadConversation,
  setAnalyzeContent
});
</script>
