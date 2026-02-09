/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
import { reactive, readonly } from 'vue';

const API_BASE_URL = '';

const state = reactive({
  token: localStorage.getItem('token') || null,
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  isLoggedIn: !!localStorage.getItem('token')
});

export const useAuthStore = () => {
  const setAuth = (token, user) => {
    state.token = token;
    state.user = user;
    state.isLoggedIn = true;
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
  };

  const clearAuth = () => {
    state.token = null;
    state.user = null;
    state.isLoggedIn = false;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  const login = async (email, password) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
      });

      const data = await response.json();

      if (response.ok) {
        setAuth(data.token, data.user);
        return { success: true, user: data.user };
      } else {
        return { success: false, message: data.error || '登录失败' };
      }
    } catch (error) {
      return { success: false, message: '网络错误' };
    }
  };

  const register = async (email, password, nickname) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password, nickname })
      });

      const data = await response.json();

      if (response.ok) {
        setAuth(data.token, data.user);
        return { success: true, user: data.user };
      } else {
        return { success: false, message: data.error || '注册失败' };
      }
    } catch (error) {
      return { success: false, message: '网络错误' };
    }
  };

  const logout = () => {
    clearAuth();
  };

  const getToken = () => state.token;

  const fetchWithAuth = async (url, options = {}) => {
    const headers = {
      ...options.headers,
      'Authorization': `Bearer ${state.token}`
    };

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
      clearAuth();
      window.location.reload();
      return null;
    }

    return response;
  };

  return {
    state: readonly(state),
    login,
    register,
    logout,
    getToken,
    fetchWithAuth,
    setAuth,
    clearAuth
  };
};
