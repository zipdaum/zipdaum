import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backendUrl = 'https://zipdaum.up.railway.app'

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
