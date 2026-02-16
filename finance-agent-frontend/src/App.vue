<template>
  <div class="app">
    <aside class="sidebar">
      <router-link to="/" class="sidebar__icon" :class="{ active: $route.name === 'chat' || $route.name === 'conversation' }" title="新对话" @click="handleNewChat">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M5 5h8a2 2 0 0 1 2 2v6H9a2 2 0 0 0-2 2v4H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
          />
          <path d="M17 11h2v6a2 2 0 0 1-2 2h-6v-2h6z" />
          <path d="M11 9h2v4h-2z" />
          <path d="M9 11h4v2H9z" />
        </svg>
      </router-link>
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
      <router-link to="/news" class="sidebar__icon" :class="{ active: $route.name === 'news' }" title="NewsNow">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/>
          <path d="M14 17H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
        </svg>
      </router-link>
      <router-link to="/market" class="sidebar__icon" :class="{ active: $route.name === 'market' }" title="市场行情">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99l1.5 1.5z"/>
        </svg>
      </router-link>
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
            <span class="topbar__brand">{{ topbarTitle }}</span>
            <span class="topbar__caret" v-if="$route.name === 'news'">▾</span>
            
            <div v-if="$route.name === 'news' && showCategoryMenu" class="category-menu">
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

      <router-view ref="currentView" @analyzeNews="handleAnalyzeNews" />
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
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AuthModal from "./components/AuthModal.vue";
import UserMenu from "./components/UserMenu.vue";
import ConversationHistory from "./components/ConversationHistory.vue";
import { useAuthStore } from "./stores/auth.js";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const showAuthModal = ref(false);
const showUserMenu = ref(false);
const showHistory = ref(false);
const currentView = ref(null);
const showCategoryMenu = ref(false);
const currentCategory = ref('finance');

const newsCategories = [
  { id: 'finance', name: '财经' },
  { id: 'china', name: '国内' },
  { id: 'world', name: '国际' },
  { id: 'tech', name: '科技' },
  { id: 'hottest', name: '最热' },
  { id: 'realtime', name: '实时' }
];

const topbarTitle = computed(() => {
  if (route.name === 'news') {
    return newsCategories.find(c => c.id === currentCategory.value)?.name || 'NewsNow';
  }
  return 'FinAgent';
});

const toggleCategoryMenu = () => {
  if (route.name === 'news') {
    showCategoryMenu.value = !showCategoryMenu.value;
  }
};

const selectCategory = (id) => {
  currentCategory.value = id;
  showCategoryMenu.value = false;
  if (currentView.value && currentView.value.selectCategory) {
    currentView.value.selectCategory(id);
  }
};

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

const handleNewChat = () => {
  if (currentView.value && currentView.value.newConversation) {
    currentView.value.newConversation();
  }
};

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

const closeHistory = () => {
  showHistory.value = false;
};

const loadConversation = async (conv) => {
  closeHistory();
  router.push({ name: 'conversation', params: { id: conv.conversationId } });
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

const handleAnalyzeNews = (data) => {
  const { title, url, prompt } = data;
  const content = prompt ? `${title}\n${prompt}` : title;
  if (currentView.value && currentView.value.setAnalyzeContent) {
    currentView.value.setAnalyzeContent(content);
  }
};
</script>
