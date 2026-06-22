import axios from 'axios'
import { clearAuth } from '../stores/auth'

const defaultApiBaseURL = import.meta.env.PROD ? 'https://zipdaum.up.railway.app' : ''
export const apiBaseURL = import.meta.env.VITE_API_BASE_URL || defaultApiBaseURL

const client = axios.create({
  baseURL: apiBaseURL
})

client.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('accessToken')

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()

      const currentPath = `${window.location.pathname}${window.location.search}`
      const loginPath = `/login?redirect=${encodeURIComponent(currentPath)}`

      if (window.location.pathname !== '/login') {
        window.location.replace(loginPath)
      }
    }

    return Promise.reject(error)
  }
)

export default client
