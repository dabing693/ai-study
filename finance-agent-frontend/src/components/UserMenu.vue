<template>
  <div class="user-menu-overlay" @click.self="close">
    <div class="user-menu">
      <div class="user-menu__header">
        <div class="user-menu__avatar">
          {{ (user?.nickname?.[0] || user?.email?.[0] || 'U').toUpperCase() }}
        </div>
        <div class="user-menu__info">
          <div class="user-menu__nickname">{{ user?.nickname || '用户' }}</div>
          <div class="user-menu__email">{{ user?.email }}</div>
        </div>
      </div>

      <div class="user-menu__stats">
        <div class="user-menu__stat">
          <span class="user-menu__stat-value">{{ registerDays }}</span>
          <span class="user-menu__stat-label">注册天数</span>
        </div>
      </div>

      <div class="user-menu__divider"></div>

      <button class="user-menu__logout" @click="handleLogout">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>
        </svg>
        退出登录
      </button>
    </div>
  </div>
</template>

<!--
  @author claude code with kimi
  @date 2026/2/6
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
  user: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['close', 'logout']);

const registerDays = computed(() => {
  if (!props.user?.createTime) return 0;
  const createDate = new Date(props.user.createTime);
  const now = new Date();
  const diffTime = now - createDate;
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
  return diffDays;
});

const close = () => {
  emit('close');
};

const handleLogout = () => {
  emit('logout');
  close();
};
</script>

<style scoped>
.user-menu-overlay {
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
  padding-left: 80px;
  padding-top: 60vh;
}

.user-menu {
  background: white;
  border-radius: 12px;
  padding: 20px;
  min-width: 240px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  animation: slideIn 0.2s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-menu__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.user-menu__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #1f1f1f;
  color: white;
  font-size: 20px;
  font-weight: 600;
  display: grid;
  place-items: center;
}

.user-menu__info {
  flex: 1;
  min-width: 0;
}

.user-menu__nickname {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-menu__email {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.user-menu__stats {
  display: flex;
  gap: 24px;
  padding: 12px 0;
}

.user-menu__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.user-menu__stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #10a37f;
}

.user-menu__stat-label {
  font-size: 12px;
  color: #888;
}

.user-menu__divider {
  height: 1px;
  background: #e0e0e0;
  margin: 12px 0;
}

.user-menu__logout {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #f5f5f5;
  color: #d32f2f;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.user-menu__logout:hover {
  background: #ffebee;
}

.user-menu__logout svg {
  width: 18px;
  height: 18px;
  fill: currentColor;
}
</style>
