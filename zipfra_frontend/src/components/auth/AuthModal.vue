<template>
  <Transition name="fade">
    <div v-if="isOpen" class="modal-overlay" @click.self="$emit('close')">
      <Transition name="slide-up">
        <div class="modal-container glass">
          <!-- Close Button -->
          <button class="close-btn" @click="$emit('close')" aria-label="Close modal">
            &times;
          </button>

          <!-- Title and Logo -->
          <div class="modal-header">
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

          <!-- Error Message Alert -->
          <Transition name="fade">
            <div v-if="errorMessage" class="alert-box error">
              <span>{{ errorMessage }}</span>
            </div>
          </Transition>

          <!-- Success Message Alert -->
          <Transition name="fade">
            <div v-if="successMessage" class="alert-box success">
              <span>{{ successMessage }}</span>
            </div>
          </Transition>

          <!-- Auth Form -->
          <form @submit.prevent="handleSubmit" class="auth-form">
            <!-- Email Field -->
            <div class="form-group">
              <label for="email">이메일 주소</label>
              <input
                id="email"
                v-model="form.email"
                type="email"
                placeholder="example@email.com"
                required
                class="form-input"
              />
            </div>

            <!-- Nickname Field (Signup Only) -->
            <Transition name="fade">
              <div v-if="!isLogin" class="form-group">
                <label for="nickname">닉네임</label>
                <input
                  id="nickname"
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

            <!-- Password Field -->
            <div class="form-group">
              <label for="password">비밀번호</label>
              <input
                id="password"
                v-model="form.password"
                type="password"
                placeholder="••••••••"
                required
                class="form-input"
              />
              <span v-if="!isLogin" class="input-hint">
                8자 이상, 영문, 숫자, 특수문자를 각각 최소 1자 이상 포함해야 합니다.
              </span>
            </div>

            <!-- Submit Button -->
            <button type="submit" class="submit-btn" :disabled="authStore.loading">
              <span v-if="authStore.loading" class="spinner"></span>
              <span v-else>{{ isLogin ? '로그인' : '회원가입 완료' }}</span>
            </button>
          </form>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { useAuthStore } from '../../stores/auth';

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits(['close']);

const authStore = useAuthStore();

const isLogin = ref(true);
const errorMessage = ref('');
const successMessage = ref('');

const form = reactive({
  email: '',
  password: '',
  nickname: '',
});

// Reset form when modal opens/closes or switches tabs
watch(() => props.isOpen, (val) => {
  if (val) {
    resetForm();
  }
});

watch(isLogin, () => {
  errorMessage.value = '';
  successMessage.value = '';
});

function resetForm() {
  form.email = '';
  form.password = '';
  form.nickname = '';
  errorMessage.value = '';
  successMessage.value = '';
}

// Client-side password policy validation for signup
function validatePassword(password) {
  const minLength = 8;
  const hasLetter = /[A-Za-z]/.test(password);
  const hasDigit = /\d/.test(password);
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
  return password.length >= minLength && hasLetter && hasDigit && hasSpecial;
}

async function handleSubmit() {
  errorMessage.value = '';
  successMessage.value = '';

  if (!isLogin.value) {
    // Signup validations
    if (!validatePassword(form.password)) {
      errorMessage.value = '비밀번호 규칙을 만족해야 합니다. (영문, 숫자, 특수문자 각 1자 이상 포함, 8자 이상)';
      return;
    }
    if (form.nickname.length < 2 || form.nickname.length > 20) {
      errorMessage.value = '닉네임은 2~20자 사이여야 합니다.';
      return;
    }

    try {
      await authStore.signup(form.email, form.password, form.nickname);
      successMessage.value = '회원가입이 완료되었습니다. 로그인해주세요!';
      isLogin.value = true;
      form.password = ''; // Clear password, keep email for convenience
    } catch (err) {
      errorMessage.value = err.message || '회원가입에 실패했습니다. 다시 시도해주세요.';
    }
  } else {
    // Login
    try {
      await authStore.login(form.email, form.password);
      emit('close');
    } catch (err) {
      errorMessage.value = err.message || '로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.';
    }
  }
}
</script>

<style scoped>
/* Modal overlay and dark backdrop */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

/* Glassmorphism container container styling */
.modal-container {
  position: relative;
  width: 100%;
  max-width: 440px;
  padding: 2.5rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 1.5rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  margin: 1rem;
}

/* Close button */
.close-btn {
  position: absolute;
  top: 1.25rem;
  right: 1.25rem;
  background: none;
  border: none;
  font-size: 1.75rem;
  color: #64748b;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s ease;
}

.close-btn:hover {
  color: #0f172a;
}

/* Logo and header */
.modal-header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo-text {
  font-size: 2.25rem;
  font-weight: 800;
  background: linear-gradient(135deg, #4f46e5 0%, #2563eb 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.25rem;
}

.subtitle-text {
  font-size: 0.875rem;
  color: #64748b;
}

/* Tab selector */
.tab-group {
  display: flex;
  background: rgba(241, 245, 249, 0.8);
  padding: 0.375rem;
  border-radius: 0.75rem;
  margin-bottom: 1.5rem;
}

.tab-btn {
  flex: 1;
  padding: 0.625rem;
  border: none;
  background: none;
  font-size: 0.95rem;
  font-weight: 600;
  color: #64748b;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.tab-btn.active {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

/* Alerts styling */
.alert-box {
  padding: 0.875rem 1rem;
  border-radius: 0.75rem;
  font-size: 0.875rem;
  margin-bottom: 1.25rem;
  line-height: 1.4;
}

.alert-box.error {
  background: rgba(254, 242, 242, 0.9);
  border: 1px solid rgba(252, 165, 165, 0.5);
  color: #dc2626;
}

.alert-box.success {
  background: rgba(236, 253, 245, 0.9);
  border: 1px solid rgba(167, 243, 208, 0.5);
  color: #059669;
}

/* Form layouts */
.auth-form {
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
  color: #334155;
}

.form-input {
  padding: 0.75rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.75rem;
  font-size: 0.95rem;
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
  outline: none;
}

.form-input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  background: #ffffff;
}

.input-hint {
  font-size: 0.75rem;
  color: #64748b;
  line-height: 1.4;
  margin-top: 0.25rem;
}

/* Button & Spinner */
.submit-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #2563eb 100%);
  color: #ffffff;
  border: none;
  padding: 0.875rem;
  font-size: 1rem;
  font-weight: 700;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 0.5rem;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.95;
  transform: translateY(-1px);
  box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.3);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
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

/* Transitions animation */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.35s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(20px);
  opacity: 0;
}
</style>
