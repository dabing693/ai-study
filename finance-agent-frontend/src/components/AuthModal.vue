<template>
  <div class="auth-modal-overlay" @click.self="close">
    <div class="auth-modal">
      <button class="auth-modal__close" @click="close">&times;</button>

      <div class="auth-modal__header">
        <h2>{{ isLogin ? '登录' : '注册' }}</h2>
      </div>

      <form class="auth-modal__form" @submit.prevent="handleSubmit">
        <div class="auth-modal__field">
          <label>邮箱</label>
          <input
            v-model="form.email"
            type="email"
            placeholder="请输入邮箱"
            required
          />
        </div>

        <div class="auth-modal__field">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            required
            minlength="6"
          />
        </div>

        <div v-if="!isLogin" class="auth-modal__field">
          <label>昵称（可选）</label>
          <input
            v-model="form.nickname"
            type="text"
            placeholder="请输入昵称"
          />
        </div>

        <div v-if="error" class="auth-modal__error">{{ error }}</div>

        <button
          type="submit"
          class="auth-modal__submit"
          :disabled="loading"
        >
          {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
        </button>
      </form>

      <div class="auth-modal__switch">
        <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <button type="button" @click="toggleMode">
          {{ isLogin ? '立即注册' : '立即登录' }}
        </button>
      </div>
    </div>
  </div>
</template>

<!--
  @author claude code with kimi
  @date 2026/2/6
-->
<script setup>
import { reactive, ref } from 'vue';
import { useAuthStore } from '../stores/auth.js';

const emit = defineEmits(['close', 'success']);

const authStore = useAuthStore();
const isLogin = ref(true);
const loading = ref(false);
const error = ref('');

const form = reactive({
  email: '',
  password: '',
  nickname: ''
});

const toggleMode = () => {
  isLogin.value = !isLogin.value;
  error.value = '';
};

const close = () => {
  emit('close');
};

const handleSubmit = async () => {
  loading.value = true;
  error.value = '';

  try {
    const result = isLogin.value
      ? await authStore.login(form.email, form.password)
      : await authStore.register(form.email, form.password, form.nickname);

    if (result.success) {
      emit('success', result.user);
      close();
    } else {
      error.value = result.message;
    }
  } catch (err) {
    error.value = '网络错误，请稍后重试';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.auth-modal {
  background: white;
  border-radius: 12px;
  padding: 32px;
  width: 100%;
  max-width: 400px;
  position: relative;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.auth-modal__close {
  position: absolute;
  top: 16px;
  right: 16px;
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

.auth-modal__close:hover {
  background: #f0f0f0;
}

.auth-modal__header {
  text-align: center;
  margin-bottom: 24px;
}

.auth-modal__header h2 {
  margin: 0;
  font-size: 24px;
  color: #1a1a1a;
}

.auth-modal__form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-modal__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.auth-modal__field label {
  font-size: 14px;
  color: #555;
  font-weight: 500;
}

.auth-modal__field input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.auth-modal__field input:focus {
  outline: none;
  border-color: #10a37f;
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.1);
}

.auth-modal__error {
  color: #e53935;
  font-size: 14px;
  text-align: center;
  padding: 8px;
  background: #ffebee;
  border-radius: 6px;
}

.auth-modal__submit {
  background: #10a37f;
  color: white;
  border: none;
  padding: 14px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
  margin-top: 8px;
}

.auth-modal__submit:hover:not(:disabled) {
  background: #0d8c6d;
}

.auth-modal__submit:disabled {
  background: #9e9e9e;
  cursor: not-allowed;
}

.auth-modal__switch {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.auth-modal__switch button {
  background: none;
  border: none;
  color: #10a37f;
  font-weight: 500;
  cursor: pointer;
  margin-left: 4px;
}

.auth-modal__switch button:hover {
  text-decoration: underline;
}
</style>
