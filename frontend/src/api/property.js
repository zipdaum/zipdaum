import client from './client'

export async function searchProperties(params) {
  const response = await client.get('/properties', { params })
  return response.data
}

export async function getPropertyRecommendations() {
  const response = await client.get('/properties/recommendations')
  return response.data
}

export async function getPropertyDetail(propertyId) {
  const response = await client.get(`/properties/${propertyId}`)
  return response.data
}

export async function getPropertyDealHistories(propertyId, params) {
  const response = await client.get(`/properties/${propertyId}/histories`, { params })
  return response.data
}

export async function getSurroundings(propertyId, params) {
  const response = await client.get(`/properties/${propertyId}/surroundings`, { params })
  return response.data
}

export async function getPropertyRecommendationScore(propertyId) {
  const response = await client.get(`/properties/${propertyId}/recommendation-score`)
  return response.data
}

export async function savePropertyInteraction(propertyId, payload) {
  await client.post(`/properties/${propertyId}/interactions`, payload)
}
