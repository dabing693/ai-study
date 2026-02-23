import { createRouter, createWebHistory } from 'vue-router'
import ReactChatView from '../views/ReactChatView.vue'
import MultiChatView from '../views/MultiChatView.vue'

const routes = [
  {
    path: '/',
    redirect: '/react-chat'
  },
  {
    path: '/react-chat',
    name: 'react-chat',
    component: ReactChatView
  },
  {
    path: '/multi-chat',
    name: 'multi-chat',
    component: MultiChatView
  },
  {
    path: '/news',
    name: 'news',
    component: () => import('../views/NewsView.vue')
  },
  {
    path: '/market',
    name: 'market',
    component: () => import('../views/MarketView.vue')
  },
  {
    path: '/conversation/:id',
    name: 'conversation',
    component: ReactChatView
  },
  {
    path: '/multi-conversation/:id',
    name: 'multi-conversation',
    component: MultiChatView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
