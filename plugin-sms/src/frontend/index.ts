import type { App } from 'vue'
import ConfigView from './views/ConfigView.vue'

export interface PluginContext {
  app: App
  router: any
  api: {
    get: (url: string, params?: any) => Promise<any>
    post: (url: string, data?: any) => Promise<any>
    put: (url: string, data?: any) => Promise<any>
    delete: (url: string) => Promise<any>
  }
  config: Record<string, any>
  notify: {
    success: (message: string) => void
    error: (message: string) => void
    info: (message: string) => void
  }
}

export interface PluginExports {
  install: (ctx: PluginContext) => void
  uninstall?: () => void
  routes?: Array<{
    path: string
    component: any
    meta?: Record<string, any>
  }>
  components?: Record<string, any>
}

const plugin: PluginExports = {
  install(ctx: PluginContext) {
    console.log('[SMS Plugin] 短信验证服务插件已加载')
  },
  
  uninstall() {
    console.log('[SMS Plugin] 短信验证服务插件已卸载')
  },
  
  routes: [
    {
      path: '/console/plugins/sms',
      component: ConfigView,
      meta: { title: '短信服务' }
    }
  ],
  
  components: {
    ConfigView
  }
}

export default plugin
