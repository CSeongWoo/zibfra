<template>
  <Transition name="fade">
    <div v-if="isOpen" class="fixed inset-0 w-full h-full bg-slate-900/40 backdrop-blur-sm flex justify-center items-center z-[9999]" @click.self="$emit('close')">
      <Transition name="slide-up">
        <Card class="relative w-full max-w-[440px] m-4 !shadow-level-2">
          <!-- Close Button -->
          <button class="absolute top-5 right-5 bg-transparent border-none text-[28px] text-primary/40 cursor-pointer leading-none transition-colors hover:text-primary" @click="$emit('close')" aria-label="Close modal">
            &times;
          </button>

          <!-- Title and Logo -->
          <div class="text-center mb-8">
            <h1 class="text-[36px] font-extrabold bg-gradient-to-br from-info to-[#3b82f6] text-transparent bg-clip-text mb-1 tracking-tight">Zipfra</h1>
            <p class="text-[14px] text-primary/60">하이브리드 공간 입지 시각화 플랫폼</p>
          </div>

          <!-- Tabs -->
          <div class="flex bg-primary/[0.03] p-1.5 rounded-md mb-6">
            <button
              v-if="!isForgotPassword"
              class="flex-1 p-2.5 border-none bg-transparent text-[15px] font-semibold text-primary/60 rounded cursor-pointer transition-all duration-250"
              :class="isLogin ? 'bg-white text-primary shadow-level-1' : ''"
              @click="isLogin = true; errorMessage = ''; successMessage = ''"
            >
              로그인
            </button>
            <button
              v-if="!isForgotPassword"
              class="flex-1 p-2.5 border-none bg-transparent text-[15px] font-semibold text-primary/60 rounded cursor-pointer transition-all duration-250"
              :class="!isLogin ? 'bg-white text-primary shadow-level-1' : ''"
              @click="isLogin = false; errorMessage = ''; successMessage = ''"
            >
              회원가입
            </button>
            <button
              v-if="isForgotPassword"
              class="flex-1 p-2.5 border-none bg-white text-primary shadow-level-1 text-[15px] font-semibold rounded cursor-default"
            >
              비밀번호 찾기
            </button>
          </div>

          <!-- Error Message Alert -->
          <Transition name="fade">
            <div v-if="errorMessage" class="p-3.5 rounded text-[14px] mb-5 leading-relaxed bg-red-50 border border-red-200 text-red-600">
              <span>{{ errorMessage }}</span>
            </div>
          </Transition>

          <!-- Success Message Alert -->
          <Transition name="fade">
            <div v-if="successMessage" class="p-3.5 rounded text-[14px] mb-5 leading-relaxed bg-green-50 border border-green-200 text-green-600">
              <span>{{ successMessage }}</span>
            </div>
          </Transition>

          <!-- Auth Form -->
          <form @submit.prevent="handleSubmit" class="flex flex-col gap-5">
            <!-- Email Field -->
            <Input 
              label="이메일 주소"
              id="email"
              v-model="form.email"
              type="email"
              placeholder="example@email.com"
              required
              :disabled="isForgotPassword && isCodeSent"
            />

            <!-- Code Field (Forgot Password Only) -->
            <Transition name="fade">
              <div v-if="isForgotPassword && isCodeSent" class="flex flex-col gap-1">
                <div class="flex items-center gap-2">
                  <Input 
                    label="인증번호"
                    id="authCode"
                    v-model="form.authCode"
                    type="text"
                    placeholder="6자리 숫자"
                    required
                    class="flex-1"
                    maxlength="6"
                  />
                </div>
                <div class="text-[12px] text-primary/60 mt-1 flex justify-between">
                  <span>남은 시간: <span class="text-rose-500 font-semibold">{{ formatTime(timeLeft) }}</span></span>
                  <button type="button" @click="handleSendCode" class="text-info hover:underline bg-transparent border-none cursor-pointer p-0" :disabled="authStore.loading">
                    재전송
                  </button>
                </div>
              </div>
            </Transition>

            <!-- Nickname Field (Signup Only) -->
            <Transition name="fade">
              <Input
                v-if="!isLogin"
                label="닉네임"
                id="nickname"
                v-model="form.nickname"
                type="text"
                placeholder="2~20자 이내"
                required
                minlength="2"
                maxlength="20"
              />
            </Transition>

            <!-- Password Field -->
            <Transition name="fade">
              <div v-if="!isForgotPassword" class="flex flex-col gap-1">
                <Input
                  label="비밀번호"
                  id="password"
                  v-model="form.password"
                  type="password"
                  placeholder="••••••••"
                  required
                />
                <span v-if="!isLogin" class="text-[12px] text-primary/60 leading-[1.4] mt-1">
                  8자 이상, 영문, 숫자, 특수문자를 각각 최소 1자 이상 포함해야 합니다.
                </span>
                <div v-if="isLogin" class="text-right mt-1">
                  <button type="button" class="text-[12px] text-info hover:underline bg-transparent border-none cursor-pointer p-0" @click="isForgotPassword = true; errorMessage = ''; successMessage = ''">
                    비밀번호를 잊으셨나요?
                  </button>
                </div>
              </div>
            </Transition>

            <!-- Submit Button -->
            <Button v-if="!(isForgotPassword && !isCodeSent)" variant="primary" type="submit" class="w-full mt-2 py-3 flex justify-center items-center" :disabled="authStore.loading">
              <span v-if="authStore.loading" class="w-5 h-5 border-2 border-white/30 rounded-full border-t-white animate-spin"></span>
              <span v-else>{{ isForgotPassword ? '임시 비밀번호 발급' : isLogin ? '로그인' : '회원가입 완료' }}</span>
            </Button>
            
            <Button v-if="isForgotPassword && !isCodeSent" variant="primary" type="button" @click="handleSendCode" class="w-full mt-2 py-3 flex justify-center items-center" :disabled="authStore.loading">
              <span v-if="authStore.loading" class="w-5 h-5 border-2 border-white/30 rounded-full border-t-white animate-spin"></span>
              <span v-else>인증번호 전송</span>
            </Button>
            
            <div v-if="isForgotPassword" class="text-center mt-2">
              <button type="button" class="text-[13px] text-primary/60 hover:text-primary bg-transparent border-none cursor-pointer underline" @click="resetForgotPassword">
                로그인으로 돌아가기
              </button>
            </div>
          </form>
        </Card>
      </Transition>
    </div>
  </Transition>
</template>

<script setup>
import { ref, reactive, watch, onUnmounted } from 'vue';
import { useAuthStore } from '../../stores/auth';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import Card from '@/components/common/Card.vue';

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits(['close']);

const authStore = useAuthStore();

const isLogin = ref(true);
const isForgotPassword = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const form = reactive({
  email: '',
  password: '',
  nickname: '',
  authCode: '',
});

const isCodeSent = ref(false);
const timeLeft = ref(300); // 5분
let timerInterval = null;

function startTimer() {
  stopTimer();
  timeLeft.value = 300;
  timerInterval = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--;
    } else {
      stopTimer();
    }
  }, 1000);
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function resetForgotPassword() {
  isForgotPassword.value = false;
  isCodeSent.value = false;
  form.authCode = '';
  stopTimer();
  errorMessage.value = '';
  successMessage.value = '';
}

onUnmounted(() => {
  stopTimer();
});

// Reset form when modal opens/closes or switches tabs
watch(() => props.isOpen, (val) => {
  if (val) {
    resetForm();
  }
});

// Removed watcher to prevent clearing messages when switching state programmatically.

function resetForm() {
  form.email = '';
  form.password = '';
  form.nickname = '';
  form.authCode = '';
  isForgotPassword.value = false;
  isCodeSent.value = false;
  stopTimer();
  errorMessage.value = '';
  successMessage.value = '';
}

function validatePassword(password) {
  const minLength = 8;
  const hasLetter = /[A-Za-z]/.test(password);
  const hasDigit = /\d/.test(password);
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~]/.test(password);
  return password.length >= minLength && hasLetter && hasDigit && hasSpecial;
}

async function handleSendCode() {
  errorMessage.value = '';
  successMessage.value = '';
  if (!form.email) {
    errorMessage.value = '이메일을 입력해주세요.';
    return;
  }
  try {
    await authStore.sendAuthCode(form.email);
    successMessage.value = '인증번호가 이메일로 전송되었습니다. 5분 안에 입력해주세요.';
    isCodeSent.value = true;
    startTimer();
  } catch (err) {
    errorMessage.value = err.response?.data?.message || err.message || '인증번호 전송에 실패했습니다.';
  }
}

async function handleSubmit() {
  errorMessage.value = '';
  successMessage.value = '';

  if (isForgotPassword.value) {
    if (!form.email || !form.authCode) {
      errorMessage.value = '이메일과 인증번호를 모두 입력해주세요.';
      return;
    }
    if (timeLeft.value === 0) {
      errorMessage.value = '인증번호 입력 시간이 초과되었습니다. 재전송해주세요.';
      return;
    }
    try {
      const tempPw = await authStore.forgotPassword(form.email, form.authCode);
      resetForgotPassword();
      isLogin.value = true;
      successMessage.value = `임시 비밀번호가 발급되었습니다: ${tempPw}`;
    } catch (err) {
      errorMessage.value = err.response?.data?.message || err.message || '인증 및 임시 비밀번호 발급에 실패했습니다.';
    }
    return;
  }

  if (!isLogin.value) {
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
      errorMessage.value = err.response?.data?.message || err.message || '회원가입에 실패했습니다. 다시 시도해주세요.';
    }
  } else {
    try {
      await authStore.login(form.email, form.password);
      emit('close');
    } catch (err) {
      errorMessage.value = err.response?.data?.message || err.message || '로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.';
    }
  }
}
</script>

<style scoped>
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
