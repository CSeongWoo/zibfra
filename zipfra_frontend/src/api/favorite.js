import http from './http';

export async function toggleFavorite(propertyId) {
  const response = await http.post(`/favorites/${propertyId}`);
  return response.data;
}

export async function getFavorites() {
  const response = await http.get('/favorites');
  return response.data;
}
