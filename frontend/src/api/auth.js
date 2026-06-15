import axios from 'axios'

const authClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || ''
})

export async function login(credentials) {
  const response = await authClient.post('/auth/login', credentials)
  return response.data
}
