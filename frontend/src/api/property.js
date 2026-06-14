import axios from 'axios'

const propertyClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || ''
})

export async function searchProperties(params) {
  const response = await propertyClient.get('/properties', { params })
  return response.data
}

export async function getPropertyDetail(propertyId) {
  const response = await propertyClient.get(`/properties/${propertyId}`)
  return response.data
}

export async function getPropertyDealHistories(propertyId) {
  const response = await propertyClient.get(`/properties/${propertyId}/histories`)
  return response.data
}
