<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content animate-fade-in">
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
          <button @click="goToLogin" class="login-btn">로그인</button>
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
  background: rgba(4, 22, 39, 0.5);
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
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #c4c6cd;
  box-shadow: 0px 12px 32px rgba(26, 43, 60, 0.12);
  overflow: hidden;
}

.modal-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e1e3e4;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
}

.modal-header h2 {
  font-size: 1.1rem;
  font-weight: 700;
  color: #041627;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #74777d;
  transition: color 0.15s ease;
  line-height: 1;
}

.close-btn:hover {
  color: #041627;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
}

.review-write-section h3,
.review-list-section h3 {
  font-size: 14px;
  font-weight: 700;
  color: #041627;
  margin-bottom: 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #44474c;
  letter-spacing: 0.04em;
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  background: #f3f4f5;
  border: 1px solid #c4c6cd;
  border-radius: 8px;
  color: #191c1d;
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #944a00;
  box-shadow: 0 0 0 2px rgba(252, 143, 52, 0.2);
  background: #ffffff;
}

.submit-btn {
  background: #041627;
  color: #ffffff;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  box-shadow: 0 0 8px rgba(252, 143, 52, 0.4);
  background: #1a2b3c;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.divider {
  border: 0;
  border-top: 1px solid #e1e3e4;
  margin: 1.5rem 0;
}

/* 리뷰 아이템 */
.review-item {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 12px;
  border: 1px solid #e1e3e4;
  margin-bottom: 0.75rem;
}

.review-item:last-child {
  margin-bottom: 0;
}

.review-header {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  align-items: center;
}

.nickname {
  font-weight: 700;
  color: #041627;
}

.rating {
  color: #944a00;
  font-weight: 600;
  font-size: 12px;
}

.date {
  color: #74777d;
  margin-left: auto;
  font-size: 12px;
}

.content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: #191c1d;
  margin: 0;
}

/* 로딩 / 빈 목록 */
.loading,
.empty {
  text-align: center;
  padding: 2rem;
  font-size: 14px;
  color: #74777d;
}

/* 페이지네이션 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.pagination button {
  padding: 6px 14px;
  background: #ffffff;
  border: 1px solid #c4c6cd;
  border-radius: 8px;
  color: #041627;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pagination button:hover:not(:disabled) {
  border-color: #944a00;
  color: #944a00;
  background: #fff5ec;
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination span {
  font-size: 13px;
  color: #44474c;
  font-weight: 600;
}

/* 로그인 유도 */
.login-prompt {
  text-align: center;
  padding: 2rem 0;
}

.login-prompt p {
  margin-bottom: 1rem;
  color: #74777d;
  font-size: 14px;
}

.login-btn {
  background: #041627;
  color: #ffffff;
  border: none;
  padding: 8px 20px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-btn:hover {
  background: #1a2b3c;
  box-shadow: 0 0 8px rgba(252, 143, 52, 0.4);
}
</style>
