<template>
  <div class="history-overlay" @click.self="close">
    <div class="history-panel">
      <div class="history-header">
        <h3>历史对话</h3>
        <button class="history-close" @click="close">&times;</button>
      </div>

      <div class="history-list" v-if="conversations.length > 0">
        <div
          v-for="conv in conversations"
          :key="conv.conversationId"
          class="history-item"
          @click="loadConversation(conv)"
        >
          <div class="history-item__icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
            </svg>
          </div>
          <div class="history-item__content">
            <div class="history-item__title">{{ conv.title }}</div>
            <div class="history-item__time">{{ formatTime(conv.updateTime) }}</div>
          </div>
          <button
            class="history-item__delete"
            @click.stop="deleteConversation(conv)"
            title="删除"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="history-empty" v-else>
        <p>暂无历史对话</p>
      </div>
    </div>
  </div>
</template>

<!--
  @author claude code with kimi
  @date 2026/2/6
-->
<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth.js';

const emit = defineEmits(['close', 'load']);

const authStore = useAuthStore();
const conversations = ref([]);
const loading = ref(false);

const fetchConversations = async () => {
  if (!authStore.state.isLoggedIn) {
    return;
  }

  loading.value = true;
  try {
    const response = await authStore.fetchWithAuth('/api/conversation/list');
    if (response && response.ok) {
      const data = await response.json();
      conversations.value = data;
    }
  } catch (err) {
    console.error('获取历史对话失败:', err);
  } finally {
    loading.value = false;
  }
};

const deleteConversation = async (conv) => {
  if (!confirm('确定要删除这个对话吗？')) {
    return;
  }

  try {
    const response = await authStore.fetchWithAuth(
      `/api/conversation/${conv.conversationId}`,
      { method: 'DELETE' }
    );
    if (response && response.ok) {
      conversations.value = conversations.value.filter(
        c => c.conversationId !== conv.conversationId
      );
    }
  } catch (err) {
    console.error('删除对话失败:', err);
  }
};

const loadConversation = (conv) => {
  emit('load', conv);
  close();
};

const close = () => {
  emit('close');
};

const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now - date;
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } else if (days === 1) {
    return '昨天';
  } else if (days < 7) {
    return `${days}天前`;
  } else {
    return date.toLocaleDateString('zh-CN');
  }
};

onMounted(() => {
  fetchConversations();
});
</script>

<style scoped>
.history-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  z-index: 1000;
}

.history-panel {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  width: 320px;
  height: 100vh;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.05);
  border-right: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(0);
  }
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.history-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1a1a1a;
}

.history-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.history-close:hover {
  background: #f0f0f0;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 4px;
}

.history-item:hover {
  background: #f5f5f5;
}

.history-item__icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.history-item__icon svg {
  width: 18px;
  height: 18px;
  fill: #666;
}

.history-item__content {
  flex: 1;
  min-width: 0;
}

.history-item__title {
  font-size: 14px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.history-item__time {
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

.history-item__delete {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s, background 0.2s;
}

.history-item:hover .history-item__delete {
  opacity: 1;
}

.history-item__delete:hover {
  background: #ffebee;
}

.history-item__delete svg {
  width: 16px;
  height: 16px;
  fill: #d32f2f;
}

.history-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
  font-size: 14px;
}

@media (max-width: 900px) {
  .history-panel {
    width: 280px;
  }
}
</style>
