<template>
  <div class="flex justify-center items-center h-screen w-full bg-background animate-fade-in">
    <Card class="w-full max-w-[400px]">
      <div class="text-center mb-8">
        <h1 class="text-[36px] font-extrabold text-primary mb-1 tracking-tight">
          Zibfra
        </h1>
        <p class="text-[14px] text-primary/60">하이브리드 공간 입지 시각화 플랫폼</p>
      </div>

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

      <!-- Error Message -->
      <Transition name="fade">
        <div v-if="errorMessage" class="p-3.5 rounded text-[14px] mb-5 leading-relaxed bg-red-50 border border-red-200 text-red-600">
          {{ errorMessage }}
        </div>
      </Transition>

      <!-- Success Message -->
      <Transition name="fade">
        <div v-if="successMessage" class="p-3.5 rounded text-[14px] mb-5 leading-relaxed bg-green-50 border border-green-200 text-green-600">
          {{ successMessage }}
        </div>
      </Transition>

      <!-- Form -->
      <form @submit.prevent="handleSubmit" class="flex flex-col gap-5">
        <Input
          label="이메일 주소"
          id="login-email"
          v-model="form.email"
          type="email"
          placeholder="example@email.com"
          required
        />

        <Transition name="fade">
          <Input
            v-if="!isLogin"
            label="닉네임"
            id="login-nickname"
            v-model="form.nickname"
            type="text"
            placeholder="2~20자 이내"
            required
            minlength="2"
            maxlength="20"
          />
        </Transition>

        <Transition name="fade">
          <div v-if="!isForgotPassword" class="flex flex-col gap-1 w-full">
            <Input
              label="비밀번호"
              id="login-password"
              v-model="form.password"
              type="password"
              placeholder="••••••••"
              required
            />
            <div v-if="isLogin" class="text-right mt-1">
              <button type="button" class="text-[12px] text-info hover:underline bg-transparent border-none cursor-pointer p-0" @click="isForgotPassword = true; errorMessage = ''; successMessage = ''">
                비밀번호를 잊으셨나요?
              </button>
            </div>
          </div>
        </Transition>

        <Button variant="primary" type="submit" class="w-full mt-2 py-3 flex justify-center items-center" :disabled="authStore.loading">
          <span v-if="authStore.loading" class="w-5 h-5 border-2 border-white/30 rounded-full border-t-white animate-spin"></span>
          <span v-else>{{ isForgotPassword ? '임시 비밀번호 발급' : isLogin ? '로그인' : '회원가입 완료' }}</span>
        </Button>
        
        <div v-if="isForgotPassword" class="text-center mt-2">
          <button type="button" class="text-[13px] text-primary/60 hover:text-primary bg-transparent border-none cursor-pointer underline" @click="isForgotPassword = false; errorMessage = ''; successMessage = ''">
            로그인으로 돌아가기
          </button>
        </div>
      </form>
    </Card>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import Card from '@/components/common/Card.vue';

const authStore = useAuthStore();
const router = useRouter();

const isLogin = ref(true);
const isForgotPassword = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const form = reactive({
  email: '',
  password: '',
  nickname: '',
});

// Removed watcher to prevent clearing messages when switching state programmatically.

async function handleSubmit() {
  errorMessage.value = '';
  successMessage.value = '';

  if (isForgotPassword.value) {
    if (!form.email) {
      errorMessage.value = '이메일을 입력해주세요.';
      return;
    }
    try {
      const tempPw = await authStore.forgotPassword(form.email);
      successMessage.value = `임시 비밀번호가 발급되었습니다: ${tempPw}`;
      form.password = '';
      isForgotPassword.value = false;
      isLogin.value = true;
    } catch (err) {
      errorMessage.value = err.response?.data?.message || err.message || '임시 비밀번호 발급에 실패했습니다.';
    }
    return;
  }

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
    errorMessage.value = err.response?.data?.message || err.message || '인증 처리에 실패했습니다.';
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
</style>
