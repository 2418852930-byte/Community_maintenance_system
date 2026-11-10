import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)

// 注册 Element Plus 全部图标组件（菜单与按钮中直接使用 <el-icon><User /></el-icon>）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
// 中文语言包，保证分页、日期等组件提示为中文
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
