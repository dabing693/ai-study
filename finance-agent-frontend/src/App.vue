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
                <Markdown
                  class="chat__content"
                  :source="msg.content"
                  :breaks="false"
                />
              </div>
            </div>
            <div v-if="loading" class="chat__item assistant">
              <div class="chat__bubble">
                <div class="chat__role">助手</div>
                <div class="chat__content chat__typing">
                  <span></span><span></span><span></span>
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
import { nextTick, ref, watch } from "vue";
import Markdown from "vue3-markdown-it";

const input = ref("");
const messages = ref([]);
const loading = ref(false);
const error = ref("");
const chatList = ref(null);
const scrollAnchor = ref(null);

const normalizeAnswer = (raw) => {
  if (raw == null) {
    return "暂无可用的回答内容。";
  }
  if (typeof raw === "string") {
    return raw.trim() || "暂无可用的回答内容。";
  }
  if (typeof raw === "object") {
    const reply =
      raw.reply ||
      raw.answer ||
      raw.data ||
      raw.result ||
      (raw.message && raw.message.content) ||
      (raw.choices &&
        raw.choices[0] &&
        raw.choices[0].message &&
        raw.choices[0].message.content);
    if (typeof reply === "string") {
      return reply.trim() || "暂无可用的回答内容。";
    }
    return JSON.stringify(raw, null, 2);
  }
  return String(raw);
};

const scrollToBottom = () => {
  const list = chatList.value;
  if (!list) return;
  requestAnimationFrame(() => {
    list.scrollTop = list.scrollHeight;
  });
};

const sendMessage = async () => {
  const text = input.value.trim();
  if (!text || loading.value) return;

  error.value = "";
  messages.value.push({
    role: "user",
    roleLabel: "你",
    content: text,
  });
  input.value = "";
  loading.value = true;
  await nextTick();
  scrollToBottom();

  try {
    const url = `/react/chat?query=${encodeURIComponent(text)}`;
    const res = await fetch(url);
    const contentType = res.headers.get("content-type") || "";
    let payload;
    if (contentType.includes("application/json")) {
      payload = await res.json();
    } else {
      const body = await res.text();
      try {
        payload = JSON.parse(body);
      } catch (parseErr) {
        payload = body;
      }
    }
    const answer = normalizeAnswer(payload);
    messages.value.push({
      role: "assistant",
      roleLabel: "助手",
      content: answer,
    });
  } catch (err) {
    const message =
      err && err.message ? err.message : "请求失败，请确认后端服务是否启动。";
    if (message.toLowerCase().includes("failed to fetch")) {
      error.value = "请求失败（可能是跨域或后端未启动）。请检查后端服务。";
    } else {
      error.value = `请求失败：${message}`;
    }
  } finally {
    loading.value = false;
    await nextTick();
    scrollToBottom();
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
