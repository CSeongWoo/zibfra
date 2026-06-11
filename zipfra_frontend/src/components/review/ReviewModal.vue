<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content glass-panel animate-fade-in">
      <header class="modal-header">
        <h2>리뷰 ({{ targetType }}: {{ targetId }})</h2>
        <button class="close-btn" @click="closeModal">×</button>
      </header>
      
      <div class="modal-body">
        <!-- Review Write Section -->
        <div v-if="authStore.isAuthenticated" class="review-write-section">
          <h3>리뷰 작성</h3>
          <form @submit.prevent="submitReview">
            <div class="form-group">
              <label>평점</label>
              <select v-model.number="newReview.rating" required class="form-input">
                <option value="5">5 - 아주 좋아요</option>
                <option value="4">4 - 좋아요</option>
                <option value="3">3 - 보통</option>
                <option value="2">2 - 별로에요</option>
                <option value="1">1 - 최악이에요</option>
              </select>
            </div>
            <div class="form-group">
              <label>내용 (개인정보는 자동 마스킹됩니다)</label>
              <textarea 
                v-model="newReview.content" 
                rows="4" 
                required 
                class="form-input"
                placeholder="리뷰 내용을 작성해주세요."
              ></textarea>
            </div>
            <button type="submit" class="submit-btn" :disabled="submitting">
              {{ submitting ? '작성 중...' : '작성하기' }}
            </button>
          </form>
        </div>
        <div v-else class="login-prompt">
          <p>리뷰를 작성하려면 로그인이 필요합니다.</p>
          <button @click="goToLogin" class="auth-btn login-btn">로그인</button>
        </div>

        <hr class="divider" />

        <!-- Review List Section -->
        <div class="review-list-section">
          <h3>리뷰 목록</h3>
          <div v-if="loading" class="loading">불러오는 중...</div>
          <div v-else-if="reviews.length === 0" class="empty">
            아직 작성된 리뷰가 없습니다.
          </div>
          <div v-else class="review-list">
            <div v-for="review in reviews" :key="review.id" class="review-item">
              <div class="review-header">
                <span class="nickname">{{ review.nickname }}</span>
                <span class="rating">⭐ {{ review.rating }}</span>
                <span class="date">{{ new Date(review.createdAt).toLocaleDateString() }}</span>
              </div>
              <p class="content">{{ review.content }}</p>
            </div>
            
            <div class="pagination" v-if="totalPages > 1">
              <button :disabled="page === 0" @click="changePage(page - 1)">이전</button>
              <span>{{ page + 1 }} / {{ totalPages }}</span>
              <button :disabled="!hasNext" @click="changePage(page + 1)">다음</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue';
import { getReviews, createReview } from '@/api/review';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';

const props = defineProps({
  isOpen: { type: Boolean, required: true },
  targetType: { type: String, required: true },
  targetId: { type: String, required: true }
});

const emit = defineEmits(['close']);
const authStore = useAuthStore();
const router = useRouter();

const reviews = ref([]);
const page = ref(0);
const size = 10;
const totalElements = ref(0);
const hasNext = ref(false);
const loading = ref(false);
const submitting = ref(false);

const newReview = reactive({
  rating: 5,
  content: ''
});

const totalPages = computed(() => Math.ceil(totalElements.value / size));

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    page.value = 0;
    fetchReviews();
  }
});

async function fetchReviews() {
  loading.value = true;
  try {
    const data = await getReviews(props.targetType, props.targetId, page.value, size);
    reviews.value = data.content;
    totalElements.value = data.totalElements;
    hasNext.value = data.hasNext;
  } catch (error) {
    console.error('Failed to fetch reviews', error);
  } finally {
    loading.value = false;
  }
}

async function submitReview() {
  submitting.value = true;
  try {
    await createReview(props.targetType, props.targetId, newReview.content, newReview.rating);
    newReview.content = '';
    newReview.rating = 5;
    page.value = 0;
    await fetchReviews();
  } catch (error) {
    console.error('Failed to create review', error);
    alert('리뷰 작성에 실패했습니다.');
  } finally {
    submitting.value = false;
  }
}

function changePage(newPage) {
  page.value = newPage;
  fetchReviews();
}

function closeModal() {
  emit('close');
}

function goToLogin() {
  closeModal();
  router.push('/login');
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  width: 100%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.modal-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-secondary);
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
  color: var(--text-secondary);
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-family: inherit;
  resize: vertical;
}

.submit-btn {
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  color: #fff;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: var(--radius-sm);
  font-weight: 600;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.divider {
  border: 0;
  border-top: 1px solid var(--border-color);
  margin: 1.5rem 0;
}

.review-item {
  background: var(--bg-secondary);
  padding: 1rem;
  border-radius: var(--radius-md);
  margin-bottom: 1rem;
}

.review-header {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}

.nickname {
  font-weight: 700;
}

.date {
  color: var(--text-secondary);
  margin-left: auto;
}

.content {
  font-size: 0.95rem;
  line-height: 1.5;
  white-space: pre-wrap;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.pagination button {
  padding: 0.5rem 1rem;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.auth-btn {
  padding: 6px 16px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.25s ease;
  outline: none;
}
.login-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #2563eb 100%);
  color: #ffffff;
}
.login-prompt {
  text-align: center;
  padding: 2rem 0;
}
.login-prompt p {
  margin-bottom: 1rem;
  color: var(--text-secondary);
}
</style>
