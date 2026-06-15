import client from './client'

export async function searchProperties(params) {
  const response = await client.get('/properties', { params })
  return response.data
}
