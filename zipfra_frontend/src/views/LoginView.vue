<template>
  <div class="login-view-container animate-fade-in">
    <div class="login-box glass">
      <div class="header">
        <h1 class="logo-text">Zipfra</h1>
        <p class="subtitle-text">하이브리드 공간 입지 시각화 플랫폼</p>
      </div>

      <!-- Tabs -->
      <div class="tab-group">
        <button
          :class="['tab-btn', { active: isLogin }]"
          @click="isLogin = true"
        >
          로그인
        </button>
        <button
          :class="['tab-btn', { active: !isLogin }]"
          @click="isLogin = false"
        >
          회원가입
        </button>
      </div>

      <!-- Error Message -->
      <Transition name="fade">
        <div v-if="errorMessage" class="alert error">
          {{ errorMessage }}
        </div>
      </Transition>

      <!-- Success Message -->
      <Transition name="fade">
        <div v-if="successMessage" class="alert success">
          {{ successMessage }}
        </div>
      </Transition>

      <!-- Form -->
      <form @submit.prevent="handleSubmit" class="login-form">
        <!-- Email -->
        <div class="form-group">
          <label for="login-email">이메일 주소</label>
          <input
            id="login-email"
            v-model="form.email"
            type="email"
            placeholder="example@email.com"
            required
            class="form-input"
          />
        </div>

        <!-- Nickname (Signup Only) -->
        <Transition name="fade">
          <div v-if="!isLogin" class="form-group">
            <label for="login-nickname">닉네임</label>
            <input
              id="login-nickname"
              v-model="form.nickname"
              type="text"
              placeholder="2~20자 이내"
              required
              minlength="2"
              maxlength="20"
              class="form-input"
            />
          </div>
        </Transition>

        <!-- Password -->
        <div class="form-group">
          <label for="login-password">비밀번호</label>
          <input
            id="login-password"
            v-model="form.password"
            type="password"
            placeholder="••••••••"
            required
            class="form-input"
          />
        </div>

        <button type="submit" class="submit-btn" :disabled="authStore.loading">
          <span v-if="authStore.loading" class="spinner"></span>
          <span v-else>{{ isLogin ? '로그인' : '회원가입 완료' }}</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const isLogin = ref(true);
const errorMessage = ref('');
const successMessage = ref('');

const form = reactive({
  email: '',
  password: '',
  nickname: '',
});

watch(isLogin, () => {
  errorMessage.value = '';
  successMessage.value = '';
});

async function handleSubmit() {
  errorMessage.value = '';
  successMessage.value = '';

  try {
    if (isLogin.value) {
      await authStore.login(form.email, form.password);
      router.push('/');
    } else {
      await authStore.signup(form.email, form.password, form.nickname);
      successMessage.value = '회원가입이 완료되었습니다. 로그인해주세요!';
      isLogin.value = true;
      form.password = '';
    }
  } catch (err) {
    errorMessage.value = err.message || '인증 처리에 실패했습니다.';
  }
}
</script>

<style scoped>
.login-view-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 64px);
  width: 100%;
  background-color: var(--bg-primary);
}

.login-box {
  width: 100%;
  max-width: 400px;
  padding: 2.5rem;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo-text {
  font-size: 2.25rem;
  font-weight: 800;
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.25rem;
}

.subtitle-text {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.tab-group {
  display: flex;
  background: var(--bg-secondary);
  padding: 0.375rem;
  border-radius: var(--radius-sm);
  margin-bottom: 1.5rem;
}

.tab-btn {
  flex: 1;
  padding: 0.625rem;
  border: none;
  background: none;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-secondary);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.tab-btn.active {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.alert {
  padding: 0.875rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  margin-bottom: 1.25rem;
  line-height: 1.4;
}

.alert.error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: var(--color-danger);
}

.alert.success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: var(--color-success);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-group label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.form-input {
  padding: 0.75rem 1rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 0.95rem;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.2s ease;
}

.form-input:focus {
  border-color: var(--accent-color);
}

.submit-btn {
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  color: #ffffff;
  border: none;
  padding: 0.875rem;
  font-size: 1rem;
  font-weight: 700;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: opacity 0.2s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 0.5rem;
}

.submit-btn:hover {
  opacity: 0.9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 1.25rem;
  height: 1.25rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #ffffff;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
