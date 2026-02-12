<template>
  <div class="app">
    <aside class="sidebar">
      <div class="sidebar__logo">◎</div>
      <button class="sidebar__icon" :class="{ active: currentView === 'chat' }" type="button" title="新对话" @click="switchToChat">
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
      <!-- NewsNow 按钮 -->
      <button
        class="sidebar__icon"
        :class="{ active: currentView === 'news' }"
        type="button"
        title="NewsNow"
        @click="switchToNews"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/>
          <path d="M14 17H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
        </svg>
      </button>
      <button
        class="sidebar__icon"
        type="button"
        title="历史"
        @click="openHistory"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M12 4a8 8 0 1 1-7.75 6H2.2a9.8 9.8 0 1 0 2.3-4.4L2 3v6h6l-2.3-2.3A7.8 7.8 0 0 1 12 4z"
          />
          <path d="M11 7h2v6h-2z" />
          <path d="M11 13h5v2h-5z" />
        </svg>
      </button>
      <div class="sidebar__spacer"></div>
      <button
        class="sidebar__avatar"
        :title="authStore.state.isLoggedIn ? authStore.state.user?.nickname || authStore.state.user?.email : '点击登录'"
        @click="handleAvatarClick"
      >
        {{ authStore.state.isLoggedIn ? (authStore.state.user?.nickname?.[0] || authStore.state.user?.email?.[0] || 'U').toUpperCase() : 'L' }}
      </button>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar__left">
          <div class="topbar__brand-container" @click="toggleCategoryMenu">
            <span class="topbar__brand">{{ currentView === 'news' ? currentCategoryName : 'ChatGPT' }}</span>
            <span class="topbar__caret">▾</span>
            
            <!-- 分类切换菜单 -->
            <div v-if="currentView === 'news' && showCategoryMenu" class="category-menu">
              <div 
                v-for="cat in newsCategories" 
                :key="cat.id" 
                class="category-menu__item"
                :class="{ active: currentCategory === cat.id }"
                @click.stop="selectCategory(cat.id)"
              >
                {{ cat.name }}
              </div>
            </div>
          </div>
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

      <section class="content" :class="{ 'content--news': currentView === 'news' }">
        <template v-if="currentView === 'chat'">
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
        </template>

        <div v-if="currentView === 'news'" class="news-container custom-scrollbar">
          <div class="news-grid">
            <NewsCard 
              v-for="source in newsSources" 
              :key="source.id"
              :source-id="source.id"
              :source-meta="source"
            />
          </div>
        </div>
      </section>
    </main>

    <AuthModal
      v-if="showAuthModal"
      @close="closeAuthModal"
      @success="handleAuthSuccess"
    />

    <UserMenu
      v-if="showUserMenu"
      :user="authStore.state.user"
      @close="closeUserMenu"
      @logout="logout"
    />

    <ConversationHistory
      v-if="showHistory"
      @close="closeHistory"
      @load="loadConversation"
    />
  </div>
</template>

<script setup>
/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import Markdown from "vue3-markdown-it";
import AuthModal from "./components/AuthModal.vue";
import UserMenu from "./components/UserMenu.vue";
import ConversationHistory from "./components/ConversationHistory.vue";
import NewsCard from "./components/NewsCard.vue";
import { useAuthStore } from "./stores/auth.js";

const authStore = useAuthStore();
const showAuthModal = ref(false);
const showUserMenu = ref(false);
const showHistory = ref(false);
const currentView = ref('chat'); // 'chat' | 'news'

const switchToChat = () => {
  currentView.value = 'chat';
  newConversation();
};

const switchToNews = () => {
  currentView.value = 'news';
};

const currentCategory = ref('finance');
const showCategoryMenu = ref(false);

const newsCategories = [
  { id: 'finance', name: '财经' },
  { id: 'china', name: '国内' },
  { id: 'world', name: '国际' },
  { id: 'tech', name: '科技' },
  { id: 'hottest', name: '最热' },
  { id: 'realtime', name: '实时' }
];

const categorySources = {
  finance: [
    { id: 'wallstreetcn-hot', name: '华尔街见闻', color: 'blue', title: '最热' },
    { id: 'cls-telegraph', name: '财联社', color: 'red', title: '电报' },
    { id: 'jin10', name: '金十数据', color: 'blue' },
    { id: 'xueqiu-hotstock', name: '雪球', color: 'blue', title: '热门股票' },
    { id: 'gelonghui', name: '格隆汇', color: 'blue', title: '事件' },
    { id: 'mktnews-flash', name: 'MKTNews', color: 'indigo', title: '快讯' },
    { id: 'wallstreetcn-news', name: '华尔街见闻', color: 'blue', title: '最新' },
    { id: 'fastbull-news', name: '法布财经', color: 'emerald', title: '头条' },
    { id: 'cls-depth', name: '财联社', color: 'red', title: '深度' },
    { id: 'cls-hot', name: '财联社', color: 'red', title: '热门' }
  ],
  china: [
    { id: 'weibo', name: '微博', color: 'red', title: '实时热搜' },
    { id: 'zhihu', name: '知乎', color: 'blue' },
    { id: 'baidu', name: '百度热搜', color: 'blue' },
    { id: 'toutiao', name: '今日头条', color: 'red' },
    { id: 'bilibili-hot-search', name: '哔哩哔哩', color: 'blue', title: '热搜' },
    { id: 'thepaper', name: '澎湃新闻', color: 'gray', title: '热榜' }
  ],
  tech: [
    { id: '36kr-renqi', name: '36氪', color: 'blue', title: '人气榜' },
    { id: 'github-trending-today', name: 'Github', color: 'gray', title: 'Today' },
    { id: 'ithome', name: 'IT之家', color: 'red' },
    { id: 'sspai', name: '少数派', color: 'red' },
    { id: 'juejin', name: '稀土掘金', color: 'blue' },
    { id: 'hackernews', name: 'Hacker News', color: 'orange' }
  ],
  world: [
    { id: 'zaobao', name: '联合早报', color: 'red', title: '实时' },
    { id: 'sputniknewscn', name: '卫星通讯社', color: 'orange' },
    { id: 'cankaoxiaoxi', name: '参考消息', color: 'red' },
    { id: 'kaopu', name: '靠谱新闻', color: 'gray' },
    { id: 'steam', name: 'Steam', color: 'blue', title: '在线人数' }
  ],
  hottest: [
    { id: 'weibo', name: '微博', color: 'red', title: '实时热搜' },
    { id: 'zhihu', name: '知乎', color: 'blue' },
    { id: 'douyin', name: '抖音', color: 'gray' },
    { id: 'kuaishou', name: '快手', color: 'orange' },
    { id: 'baidu', name: '百度热搜', color: 'blue' }
  ],
  realtime: [
    { id: 'cls-telegraph', name: '财联社', color: 'red', title: '电报' },
    { id: 'jin10', name: '金十数据', color: 'blue' },
    { id: 'wallstreetcn-quick', name: '华尔街见闻', color: 'blue', title: '快讯' },
    { id: '36kr-quick', name: '36氪', color: 'blue', title: '快讯' },
    { id: 'ithome', name: 'IT之家', color: 'red' }
  ]
};

const currentCategoryName = computed(() => {
  return newsCategories.find(c => c.id === currentCategory.value)?.name || 'NewsNow';
});

const newsSources = computed(() => {
  return categorySources[currentCategory.value] || [];
});

const toggleCategoryMenu = () => {
  if (currentView.value === 'news') {
    showCategoryMenu.value = !showCategoryMenu.value;
  }
};

const selectCategory = (id) => {
  currentCategory.value = id;
  showCategoryMenu.value = false;
};

// 点击外部关闭菜单
const closeMenuHandler = (e) => {
  if (!e.target.closest('.topbar__brand-container')) {
    showCategoryMenu.value = false;
  }
};

onMounted(() => {
  window.addEventListener('click', closeMenuHandler);
});

onBeforeUnmount(() => {
  window.removeEventListener('click', closeMenuHandler);
});

const openAuthModal = () => {
  showAuthModal.value = true;
};

const closeAuthModal = () => {
  showAuthModal.value = false;
};

const handleAuthSuccess = (user) => {
  console.log("登录成功:", user);
};

const openHistory = () => {
  if (!authStore.state.isLoggedIn) {
    showAuthModal.value = true;
    return;
  }
  showHistory.value = true;
};

const newConversation = () => {
  conversationId.value = '';
  messages.value = [];
  const url = new URL(window.location.href);
  url.searchParams.delete('conversation');
  window.history.replaceState(null, '', url.toString());
};

const closeHistory = () => {
  showHistory.value = false;
};

const loadConversation = async (conv) => {
  conversationId.value = conv.conversationId;
  messages.value = [];

  // 更新 URL
  const url = new URL(window.location.href);
  url.searchParams.set("conversation", conv.conversationId);
  window.history.replaceState(null, "", url.toString());

  // 加载历史消息
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

const handleAvatarClick = () => {
  if (authStore.state.isLoggedIn) {
    showUserMenu.value = true;
  } else {
    showAuthModal.value = true;
  }
};

const closeUserMenu = () => {
  showUserMenu.value = false;
};

const logout = () => {
  authStore.logout();
};

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

const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content);
    console.log('内容已复制到剪贴板');
  } catch (err) {
    console.error('复制失败:', err);
  }
};

/**
 * 获取助手消息的展示内容，与后端 storedContent() 方法逻辑保持一致
 * 组合 reasoningContent、content 和 toolCalls
 */
const getAssistantDisplayContent = (msg) => {
  if (!msg || msg.role !== 'assistant') return msg?.content || '';

  const reasoningContent = msg.reasoningContent ? `思考内容：\n${msg.reasoningContent}` : '';
  const content = msg.content || '';
  const toolCalls = msg.toolCalls || [];

  // 组合内容：reasoningContent + content
  let fullContent = reasoningContent;
  if (content) {
    fullContent = reasoningContent ? `${reasoningContent}\n\n${content}` : content;
  }

  // 如果有 toolCalls，追加到内容中
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

/**
 * 解析历史消息数据，统一处理用户、助手和工具消息
 * @param {Array} historyMessages - 从后端获取的原始消息数组
 * @returns {Array} 解析后的消息数组
 */
const parseHistoryMessages = (historyMessages) => {
  const parsedMessages = [];
  let lastAssistantMsg = null;

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
        lastAssistantMsg = null;
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
        lastAssistantMsg = assistantMsg;
      } else if (msg.type === 'tool') {
        // Tool Result 作为独立消息
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

  // 获取 composer 元素，用于计算滚动偏移
  const composerEl = document.querySelector('.composer');
  const composerHeight = composerEl ? composerEl.offsetHeight + 32 : 120; // 32px 是底部间距，默认 120px

  requestAnimationFrame(() => {
    // 获取最后一个消息元素
    const lastMessage = list.querySelector('.chat__item:last-of-type');
    if (lastMessage) {
      // 计算最后一个消息元素相对于 list 的偏移
      const messageBottom = lastMessage.offsetTop + lastMessage.offsetHeight;
      // 需要滚动的位置：消息底部 + composer 高度 - list 可视高度
      let targetScrollTop = messageBottom + composerHeight - list.clientHeight;
      targetScrollTop = Math.max(0, targetScrollTop);
      list.scrollTop = targetScrollTop;
    } else {
      // 兜底：直接滚到底部加上足够的间距
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

const updateConversationUrl = (conversationId) => {
  const url = new URL(window.location.href);
  url.searchParams.set("conversation", conversationId);
  window.history.replaceState(null, "", url.toString());
};

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

  // 用于跟踪当前活跃的 assistant 消息（支持多次调用生成多个消息块）
  let currentAssistantMessage = assistantMessage;

  // 创建新的 assistant 消息块
  const createNewAssistantMessage = (index) => {
    // 先结束之前的消息
    if (currentAssistantMessage) {
      currentAssistantMessage.streaming = false;
    }
    // 创建新消息
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
    updateConversationUrl(conversationId.value);
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
          // 将 toolCall 添加到当前 assistant 消息的 toolCalls 数组中
          // Tool Call 是模型返回的一部分，属于助手消息块
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
          // Tool Result 是工具执行的结果，作为独立消息
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

// 页面加载时检查 URL 中的 conversation 参数
onMounted(async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const convId = urlParams.get('conversation');
  if (convId) {
    conversationId.value = convId;
    // 尝试加载历史对话
    if (authStore.state.isLoggedIn) {
      try {
        const response = await authStore.fetchWithAuth(
          `/api/conversation/${convId}/messages`
        );
        if (response && response.ok) {
          const historyMessages = await response.json();
          if (historyMessages && historyMessages.length > 0) {
            // 有历史数据，正常显示
            messages.value = parseHistoryMessages(historyMessages);
            return;
          }
        }
        // 查询失败或没有数据，清除 conversation 参数
        newConversation();
      } catch (err) {
        console.error('检查历史对话失败:', err);
        newConversation();
      }
    } else {
      // 未登录但有 conversation 参数，清除
      newConversation();
    }
  }
});

watch(
  () => messages.value.length,
  async () => {
    await nextTick();
    scrollToBottom();
  }
);
</script>
