import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({

  server: {
    port: 4000,
    host: '0.0.0.0'
  },
  plugins: [vue()],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/frontend/index.ts'),
      name: 'MyPlugin',
      fileName: 'index',
      formats: ['es']
    },
    rollupOptions: {
      external: ['vue', 'vue-router', 'pinia'],
      output: {
        globals: {
          vue: 'Vue',
          'vue-router': 'VueRouter',
          pinia: 'Pinia'
        }
      }
    },
    outDir: 'dist',
    emptyOutDir: true
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  }
})
