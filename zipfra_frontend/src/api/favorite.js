import http from './http';

export async function toggleFavorite(propertyId) {
  const response = await http.post(`/api/v1/favorites/${propertyId}`);
  return response.data;
}

export async function getFavorites() {
  const response = await http.get('/api/v1/favorites');
  return response.data;
}
