<template>
  <section class="content content--news">
    <div class="news-container custom-scrollbar">
      <div class="news-grid">
        <NewsCard 
          v-for="source in newsSources" 
          :key="source.id"
          :source-id="source.id"
          :source-meta="source"
          @analyze="handleAnalyzeNews"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue';
import NewsCard from '../components/NewsCard.vue';

const emit = defineEmits(['analyzeNews']);

const currentCategory = ref('finance');

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

const selectCategory = (id) => {
  currentCategory.value = id;
};

const handleAnalyzeNews = (data) => {
  emit('analyzeNews', data);
};

defineExpose({
  currentCategory,
  currentCategoryName,
  newsCategories,
  selectCategory
});
</script>
