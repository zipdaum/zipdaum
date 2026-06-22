import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backendUrl = 'http://127.0.0.1:8080'
// TODO: 배포 환경 확인 시 Railway 백엔드로 전환
// const backendUrl = 'https://zipdaum.up.railway.app'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/properties': {
        target: backendUrl,
        changeOrigin: true
      },
      '/auth': {
        target: backendUrl,
        changeOrigin: true
      },
      '/users': {
        target: backendUrl,
        changeOrigin: true
      },
      '/api': {
        target: backendUrl,
        changeOrigin: true
      }
    }
  }
})
