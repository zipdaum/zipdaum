import client from './client'

export async function runPropertyBatchByRange(params) {
  const response = await client.get('/api/admin/batch/property/manual', { params })
  return response.data
}
