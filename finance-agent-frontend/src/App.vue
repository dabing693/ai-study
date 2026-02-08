<template>
  <div class="app">
    <aside class="sidebar">
      <div class="sidebar__logo">◎</div>
      <button class="sidebar__icon" type="button" title="新对话">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M5 5h8a2 2 0 0 1 2 2v6H9a2 2 0 0 0-2 2v4H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
          />
          <path d="M17 11h2v6a2 2 0 0 1-2 2h-6v-2h6z" />
          <path d="M11 9h2v4h-2z" />
          <path d="M9 11h4v2H9z" />
        </svg>
      </button>
      <button class="sidebar__icon" type="button" title="搜索">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M10.5 3a7.5 7.5 0 1 0 4.74 13.34l3.7 3.7 1.41-1.41-3.7-3.7A7.5 7.5 0 0 0 10.5 3zm0 2a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11z"
          />
        </svg>
      </button>
      <button class="sidebar__icon" type="button" title="历史">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M12 4a8 8 0 1 1-7.75 6H2.2a9.8 9.8 0 1 0 2.3-4.4L2 3v6h6l-2.3-2.3A7.8 7.8 0 0 1 12 4z"
          />
          <path d="M11 7h2v6h-2z" />
          <path d="M11 13h5v2h-5z" />
        </svg>
      </button>
      <div class="sidebar__spacer"></div>
      <div class="sidebar__avatar">Y</div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar__left">
          <span class="topbar__brand">ChatGPT</span>
          <span class="topbar__caret">▾</span>
        </div>
        <div class="topbar__right">
          <button class="chip" type="button">获取 Plus</button>
          <button class="icon-btn" type="button" title="分享">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M12 3 8 7h3v6h2V7h3l-4-4zM5 11v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6h-2v6H7v-6H5z"
              />
            </svg>
          </button>
          <button class="icon-btn" type="button" title="设置">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm8 3h-2.1a6.7 6.7 0 0 0-.8-1.9l1.5-1.5-1.4-1.4-1.5 1.5a6.7 6.7 0 0 0-1.9-.8V4h-2v2.1a6.7 6.7 0 0 0-1.9.8L7.6 5.4 6.2 6.8l1.5 1.5a6.7 6.7 0 0 0-.8 1.9H4v2h2.1a6.7 6.7 0 0 0 .8 1.9l-1.5 1.5 1.4 1.4 1.5-1.5a6.7 6.7 0 0 0 1.9.8V20h2v-2.1a6.7 6.7 0 0 0 1.9-.8l1.5 1.5 1.4-1.4-1.5-1.5a6.7 6.7 0 0 0 .8-1.9H20v-2z"
              />
            </svg>
          </button>
        </div>
      </header>

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
                  v-if="msg.role === 'assistant' && msg.streaming && !msg.content"
                  class="chat__content chat__typing"
                >
                  <span></span><span></span><span></span>
                </div>
                <Markdown
                  v-else
                  class="chat__content"
                  :source="msg.renderedContent || msg.content"
                  :breaks="false"
                />
              </div>
            </div>
            <div ref="scrollAnchor"></div>
          </div>
        </div>

        <div class="composer">
          <div class="composer__input">
            <span class="composer__plus">＋</span>
            <textarea
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
    </main>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import Markdown from "vue3-markdown-it";

const input = ref("");
const messages = ref([]);
const loading = ref(false);
const error = ref("");
const chatList = ref(null);
const scrollAnchor = ref(null);
const conversationId = ref("");
let sseController = null;
let pendingScroll = false;
let flushTimer = null;
const FLUSH_INTERVAL = 50;
let autoScrollTimer = null;
let resizeObserver = null;
const streamingActive = ref(false);

const scrollToBottom = () => {
  const anchor = scrollAnchor.value;
  if (anchor && anchor.scrollIntoView) {
    anchor.scrollIntoView({ behavior: "auto", block: "end" });
    return;
  }
  const list = chatList.value;
  if (!list) return;
  requestAnimationFrame(() => {
    const maxScrollTop = Math.max(0, list.scrollHeight - list.clientHeight);
    list.scrollTop = maxScrollTop;
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

const scheduleFlush = (message) => {
  if (!message || !message.streaming) return;
  if (flushTimer) return;
  flushTimer = setTimeout(async () => {
    flushTimer = null;
    message.renderedContent = message.content;
    await nextTick();
    scheduleScroll();
  }, FLUSH_INTERVAL);
};

const buildUuid = () => crypto.randomUUID().replace(/-/g, "");

const updateConversationUrl = (conversationId) => {
  const url = new URL(window.location.href);
  url.searchParams.set("conversation", conversationId);
  window.history.replaceState(null, "", url.toString());
};

const finalizeStreamingState = async (assistantMessage) => {
  assistantMessage.streaming = false;
  assistantMessage.renderedContent = assistantMessage.content;
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
    renderedContent: "",
    streaming: true,
  });
  messages.value.push(assistantMessage);
  await nextTick();
  scrollToBottom();
  streamingActive.value = true;
  startAutoScroll();
  const insertToolMessage = (toolMsg) => {
    const idx = messages.value.indexOf(assistantMessage);
    if (idx >= 0) {
      messages.value.splice(idx, 0, toolMsg);
    } else {
      messages.value.push(toolMsg);
    }
  };

  let isNewSession = false;
  if (!conversationId.value) {
    conversationId.value = buildUuid();
    isNewSession = true;
    updateConversationUrl(conversationId.value);
  }

  const url = new URL("/react/chat/stream", window.location.origin);
  url.searchParams.set("query", text);
  if (conversationId.value) {
    url.searchParams.set("conversationId", conversationId.value);
  }

  sseController = new AbortController();
  try {
    const response = await fetch(url.toString(), {
      headers: {
        isNew: isNewSession ? "true" : "false",
      },
      signal: sseController.signal,
    });

    if (!response.ok) {
      throw new Error(`Request failed with ${response.status}`);
    }

    await nextTick();
    scrollToBottom();

    await parseSseStream(response, async (eventName, data) => {
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
      if (eventName === "delta") {
        try {
          const payload = JSON.parse(data);
          if (payload.content) {
            assistantMessage.content += payload.content;
            scheduleFlush(assistantMessage);
          }
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "tool_call") {
        try {
          const payload = JSON.parse(data);
          insertToolMessage({
            role: "tool",
            roleLabel: "Tool Call",
            content:
              "Call " +
              (payload.toolName || "Tool") +
              "\n" +
              (payload.toolArguments || ""),
          });
          await nextTick();
          scrollToBottom();
        } catch (err) {
          return;
        }
        return;
      }
      if (eventName === "tool_result") {
        try {
          const payload = JSON.parse(data);
          insertToolMessage({
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
        await finalizeStreamingState(assistantMessage);
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
        await finalizeStreamingState(assistantMessage);
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
    await finalizeStreamingState(assistantMessage);
    if (sseController) {
      sseController.abort();
      sseController = null;
    }
  }
};

watch(
  () => messages.value.length,
  async () => {
    await nextTick();
    scrollToBottom();
  }
);
</script>
