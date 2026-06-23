import http from './http';

export async function getReviews(targetType, targetId, page = 0, size = 10) {
  const response = await http.get('/reviews', {
    params: { targetType, targetId, page, size }
  });
  return response.data;
}

export async function createReview(targetType, targetId, content, rating) {
  const response = await http.post('/reviews', {
    targetType,
    targetId,
    content,
    rating
  });
  return response.data;
}
