import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

const apiPaths = ['/properties', '/auth', '/users', '/api']

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendUrl = env.VITE_API_PROXY_TARGET || env.VITE_API_BASE_URL || 'http://localhost:8080'

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: Object.fromEntries(
        apiPaths.map((path) => [
          path,
          {
            target: backendUrl,
            changeOrigin: true
          }
        ])
      )
    }
  }
})
