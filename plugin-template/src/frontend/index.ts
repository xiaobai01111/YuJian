import type { App } from 'vue'
import MainView from './views/MainView.vue'

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
    console.log('[MyPlugin] 插件已加载', ctx.config)
  },
  
  uninstall() {
    console.log('[MyPlugin] 插件已卸载')
  },
  
  routes: [
    {
      path: '/console/plugins/my-plugin',
      component: MainView,
      meta: { title: '示例插件' }
    }
  ],
  
  components: {
    MainView
  }
}

export default plugin
