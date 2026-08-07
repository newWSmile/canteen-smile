import 'element-plus/dist/index.css'
import '@/assets/styles/main.css'

import ElementPlus from 'element-plus'
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './app/router'
import { store } from './app/store'

createApp(App).use(ElementPlus).use(store).use(router).mount('#app')
