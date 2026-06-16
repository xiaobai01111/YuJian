import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const httpsEnabled = env.VITE_HTTPS === 'true'
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  const resolveHttpsFile = (value: string | undefined, name: string) => {
    if (!value) {
      throw new Error(`${name} is required when VITE_HTTPS=true`)
    }
    return fs.readFileSync(path.resolve(process.cwd(), value))
  }

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src_console', import.meta.url))
      }
    },
    server: {
      port: 5173,
      host: '0.0.0.0',
      https: httpsEnabled
        ? {
            key: resolveHttpsFile(env.VITE_HTTPS_KEY, 'VITE_HTTPS_KEY'),
            cert: resolveHttpsFile(env.VITE_HTTPS_CERT, 'VITE_HTTPS_CERT'),
            ca: env.VITE_HTTPS_CA ? resolveHttpsFile(env.VITE_HTTPS_CA, 'VITE_HTTPS_CA') : undefined
          }
        : undefined,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          xfwd: true
        },
        '/uploads': {
          target: apiTarget,
          changeOrigin: true,
          xfwd: true
        }
      }
    }
  }
})
