<template>
  <div class="market-dashboard custom-scrollbar">
    <div class="dashboard-header">
      <h2>实时行情看板</h2>
      <button class="refresh-btn" @click="fetchData" :disabled="loading">
        <span v-if="loading">刷新中...</span>
        <span v-else>↻ 刷新数据</span>
      </button>
    </div>

    <div v-if="error" class="error-msg">{{ error }}</div>

    <div class="grid-container">
      <!-- 热门股票 -->
      <section class="card">
        <h3>🔥 热门股票排行</h3>
        <div class="table-wrapper">
          <table v-if="data.hotStocks.length">
            <thead>
              <tr>
                <th v-for="col in data.hotStocks[0]" :key="col">{{ col }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in data.hotStocks.slice(1)" :key="i">
                <td v-for="(cell, j) in row" :key="j" :class="getPriceClass(cell, j, 5)">
                  {{ cell }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 行业板块 -->
      <section class="card">
        <h3>轮动板块</h3>
        <div class="table-wrapper">
          <table v-if="data.hotIndustries.length">
            <thead>
              <tr>
                <th v-for="col in data.hotIndustries[0]" :key="col">{{ col }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in data.hotIndustries.slice(1)" :key="i">
                <td v-for="(cell, j) in row" :key="j" :class="getPriceClass(cell, j, 2)">
                  {{ cell }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 龙虎榜 -->
      <section class="card full-width">
        <h3>龙虎榜动向 (最新交易日)</h3>
        <div class="table-wrapper">
          <table v-if="data.lhbData.length">
            <thead>
              <tr>
                <th v-for="col in data.lhbData[0]" :key="col">{{ col }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in data.lhbData.slice(1)" :key="i">
                <td v-for="(cell, j) in row" :key="j" :class="getPriceClass(cell, j, 5)">
                  {{ cell }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';

const loading = ref(false);
const error = ref('');
const data = reactive({
  hotStocks: [],
  hotIndustries: [],
  lhbData: []
});

const getPriceClass = (val, colIdx, targetIdx) => {
  if (colIdx !== targetIdx) return '';
  const num = parseFloat(val);
  if (isNaN(num)) return '';
  return num > 0 ? 'up' : num < 0 ? 'down' : '';
};

const fetchData = async () => {
  loading.value = true;
  error.value = '';
  try {
    const response = await fetch('/api/market/overview');
    if (!response.ok) throw new Error('获取数据失败');
    const json = await response.json();
    data.hotStocks = json.hotStocks || [];
    data.hotIndustries = json.hotIndustries || [];
    data.lhbData = json.lhbData || [];
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);
</script>

<style scoped>
.market-dashboard {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background: #f9f9f9;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.refresh-btn {
  padding: 8px 16px;
  background: #10a37f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.grid-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card {
  background: white;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.full-width {
  grid-column: span 2;
}

h3 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 1.1rem;
  color: #333;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

th, td {
  text-align: left;
  padding: 10px;
  border-bottom: 1px solid #eee;
}

th {
  background: #f4f4f4;
  font-weight: 600;
}

.up { color: #d20000; font-weight: bold; }
.down { color: #008100; font-weight: bold; }

.error-msg {
  color: #ff4d4f;
  margin-bottom: 15px;
}
</style>
