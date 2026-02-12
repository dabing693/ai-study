<template>
  <div 
    class="news-card" 
    :style="cardStyle"
  >
    <div class="news-card__header">
      <div class="news-card__source">
        <div 
          class="news-card__icon" 
          :style="{ backgroundImage: `url(/news-icons/${sourceId.split('-')[0]}.png)` }"
        ></div>
        <div class="news-card__info">
          <div class="news-card__name-wrapper">
            <span class="news-card__name">{{ sourceMeta.name }}</span>
            <span v-if="sourceMeta.title" class="news-card__tag" :style="tagStyle">
              {{ sourceMeta.title }}
            </span>
          </div>
          <div class="news-card__time">{{ updatedText }}</div>
        </div>
      </div>
      <div class="news-card__actions">
        <button 
          class="news-card__action-btn" 
          :class="{ 'is-loading': loading }"
          @click="fetchData"
          title="刷新"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" />
          </svg>
        </button>
      </div>
    </div>

    <div class="news-card__content custom-scrollbar">
      <div v-if="loading && !items.length" class="news-card__loading">
        加载中...
      </div>
      <div v-else-if="error" class="news-card__error">
        {{ error }}
      </div>
      <ul v-else class="news-card__list">
        <li v-for="(item, index) in items" :key="item.id" class="news-card__item">
          <button 
            class="news-card__ai-btn" 
            title="AI分析"
            @click.stop="handleAnalyze(item)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
          </button>
          <a :href="item.url" target="_blank" class="news-card__link">
            <span class="news-card__index">{{ index + 1 }}</span>
            <span class="news-card__item-title">{{ item.title }}</span>
            <span v-if="item.extra && item.extra.icon" class="news-card__item-extra">
              <img 
                v-if="typeof item.extra.icon === 'string'" 
                :src="item.extra.icon" 
                class="news-card__extra-icon"
              />
              <img 
                v-else-if="item.extra.icon.url" 
                :src="item.extra.icon.url" 
                class="news-card__extra-icon"
                :style="{ transform: `scale(${item.extra.icon.scale || 1})` }"
              />
            </span>
          </a>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';

const props = defineProps({
  sourceId: {
    type: String,
    required: true
  },
  sourceMeta: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['analyze']);

const items = ref([]);
const loading = ref(false);
const error = ref(null);
const updatedTime = ref(null);

const colorMap = {
  red: '#ef4444',
  blue: '#3b82f6',
  green: '#22c55e',
  orange: '#f97316',
  slate: '#64748b',
  indigo: '#6366f1',
  gray: '#6b7280',
  yellow: '#eab308',
  emerald: '#10b981',
  teal: '#14b8a6',
  primary: '#ef4444'
};

const hex2rgba = (hex, opacity) => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r}, ${g}, ${b}, ${opacity})`;
};

const cardStyle = computed(() => {
  const color = colorMap[props.sourceMeta.color] || colorMap.primary;
  return {
    backgroundColor: hex2rgba(color, 0.15),
    '--theme-color': color
  };
});

const tagStyle = computed(() => {
  return {
    color: 'var(--theme-color)',
    backgroundColor: hex2rgba(colorMap[props.sourceMeta.color] || colorMap.primary, 0.2)
  };
});

const updatedText = computed(() => {
  if (!updatedTime.value) return '加载中...';
  const now = Date.now();
  const diff = now - updatedTime.value;
  if (diff < 60000) return '刚刚更新';
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}分钟前更新`;
  const hours = Math.floor(mins / 60);
  return `${hours}小时前更新`;
});

const fetchData = async () => {
  loading.value = true;
  error.value = null;
  try {
    const response = await fetch(`/news-api/s?id=${props.sourceId}`);
    if (!response.ok) throw new Error('网络请求失败');
    const data = await response.json();
    items.value = data.items;
    updatedTime.value = data.updatedTime || Date.now();
  } catch (err) {
    console.error(`获取 ${props.sourceId} 失败:`, err);
    error.value = '加载失败';
  } finally {
    loading.value = false;
  }
};

const handleAnalyze = async (item) => {
  try {
    const response = await fetch(`/prompt/news?source=${props.sourceId.split('-')[0]}`);
    if (!response.ok) throw new Error('获取提示词失败');
    const data = await response.json();
    const prompt = data.prompt || '';
    emit('analyze', {
      title: item.title,
      url: item.url,
      prompt: prompt,
      sourceId: props.sourceId
    });
  } catch (err) {
    console.error('获取提示词失败:', err);
    emit('analyze', {
      title: item.title,
      url: item.url,
      prompt: '',
      sourceId: props.sourceId
    });
  }
};

onMounted(fetchData);

watch(() => props.sourceId, fetchData);
</script>

<style scoped>
.news-card {
  display: flex;
  flex-direction: column;
  height: 480px;
  border-radius: 20px;
  padding: 16px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.news-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.news-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.news-card__source {
  display: flex;
  align-items: center;
  gap: 10px;
}

.news-card__icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-size: cover;
  background-color: #fff;
}

.news-card__info {
  display: flex;
  flex-direction: column;
}

.news-card__name-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.news-card__name {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

.news-card__tag {
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.news-card__time {
  font-size: 11px;
  color: #666;
  opacity: 0.8;
}

.news-card__actions {
  display: flex;
  gap: 8px;
}

.news-card__action-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--theme-color);
  opacity: 0.6;
  transition: opacity 0.2s, transform 0.2s;
  padding: 4px;
}

.news-card__action-btn:hover {
  opacity: 1;
  transform: rotate(30deg);
}

.news-card__action-btn svg {
  width: 18px;
  height: 18px;
}

.news-card__action-btn.is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.news-card__content {
  flex: 1;
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  padding: 10px;
  overflow-y: auto;
}

.news-card__list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.news-card__item {
  margin-bottom: 6px;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.news-card__ai-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--theme-color);
  opacity: 0.5;
  transition: opacity 0.2s, transform 0.2s;
  padding: 2px;
  flex-shrink: 0;
  margin-top: 2px;
}

.news-card__ai-btn:hover {
  opacity: 1;
  transform: scale(1.1);
}

.news-card__ai-btn svg {
  width: 16px;
  height: 16px;
}

.news-card__link {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  text-decoration: none;
  color: inherit;
  font-size: 14.5px;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.news-card__link:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.news-card__index {
  background-color: rgba(0, 0, 0, 0.05);
  min-width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #666;
  flex-shrink: 0;
}

.news-card__item-title {
  line-height: 1.4;
  word-break: break-all;
}

.news-card__item-extra {
  margin-left: 4px;
}

.news-card__extra-icon {
  height: 14px;
  vertical-align: middle;
}

.news-card__loading, .news-card__error {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #666;
  font-size: 14px;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>
