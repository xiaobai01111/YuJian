import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import { permission } from './directives/permission'
import { useUserStore } from '@/stores/user'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)
  app.use(router)
  app.directive('permission', permission)

  const userStore = useUserStore()
  await userStore.restoreSession()

  app.mount('#app')
}

bootstrap()
