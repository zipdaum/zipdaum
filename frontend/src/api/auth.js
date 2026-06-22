import client from './client'

export async function login(credentials) {
  const response = await client.post('/auth/login', credentials)
  return response.data
}

export async function requestVerificationCode(payload) {
  const response = await client.post('/users/mail/request', payload)
  return response.data
}

export async function verifyEmailCode(payload) {
  const response = await client.post('/users/mail/verify', payload)
  return response.data
}

export async function signUp(payload) {
  const response = await client.post('/users', payload)
  return response.data
}
