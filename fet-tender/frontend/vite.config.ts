import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    headers: {
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
      'Content-Security-Policy': "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://wmts.nlsc.gov.tw; font-src 'self' data:; connect-src 'self' http://localhost:8080 ws://localhost:5173"
    },
    proxy: {
      '/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 120000,         // 120 秒，配合 LLM 回應時間
        proxyTimeout: 120000,
      }
    }
  }
})
