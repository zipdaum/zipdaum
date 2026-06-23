import client, { apiBaseURL } from './client'

function getApiUrl(path) {
  if (!apiBaseURL) {
    return path
  }
  return `${apiBaseURL.replace(/\/$/, '')}${path}`
}

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

export async function comparePropertiesByAi(payload) {
  const response = await client.post('/properties/compare/ai', payload)
  return response.data
}

export async function savePropertyInteraction(propertyId, payload) {
  await client.post(`/properties/${propertyId}/interactions`, payload)
}

export function savePropertyInteractionKeepalive(propertyId, payload) {
  const accessToken = localStorage.getItem('accessToken')
  const headers = {
    'Content-Type': 'application/json'
  }

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  fetch(getApiUrl(`/properties/${propertyId}/interactions`), {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
    keepalive: true
  }).catch(() => {})
}
